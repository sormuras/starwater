package starwater;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownAllStereo {



  // <a href="20160830_193924_d7c2A.jpg">20160830_193924_d7c2A.jpg</a>
  public static List<String> extractUrlsFromString(String content) {
    List<String> result = new ArrayList<>();

    String regex = "HREF=\"(.*jpg?)\"";

    Pattern p = Pattern.compile(regex);
    Matcher m = p.matcher(content);
    while (m.find()) {
      result.add(m.group(1));
    }

    return result;
  }

  public static void main(String[] args) throws Exception {
    Instant now = Instant.now();
    System.out.println(now);
    OffsetDateTime odt = OffsetDateTime.ofInstant(now, ZoneOffset.UTC);
    System.out.println(odt);

    DateTimeFormatter uriformat = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    DateTimeFormatter fileformat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    String camera = "cor2";
    // String camera = "euvi/195";

    // String size = "512";
    String size = "1024";

    URL website = new URL("https://stereo.gsfc.nasa.gov/browse/" + odt.format(uriformat) + "/ahead/"
        + camera + "/" + size + "/index.shtml");
    try (InputStream in = website.openStream()) {
      Files.copy(in, Paths.get(size + ".html"), StandardCopyOption.REPLACE_EXISTING);
    }
    List<String> urls =
        extractUrlsFromString(String.join("\n", Files.readAllLines(Paths.get(size + ".html"))));

    System.out.println("Found " + urls.size() + " images linked in " + website);
    Path target = Paths.get("images", odt.format(fileformat));
    Files.createDirectories(target);

    AnimatedGIFWriter writer = new AnimatedGIFWriter(true);
    OutputStream os = new FileOutputStream(
        odt.format(fileformat) + "-ahead-" + camera.replace('/', '-') + "-" + size + ".gif");
    writer.prepareForWrite(os, -1, -1);

    int index = 0;
    for (String spec : urls) {
      URL url = new URL(website, spec);
      Path jpg = target.resolve(spec);
      System.out.println(spec + " -> " + url);
      if (!jpg.toFile().exists()) {
        System.out.print(" Downloading...");
        try (InputStream in = url.openStream()) {
          Files.copy(in, jpg, StandardCopyOption.REPLACE_EXISTING);
        }
        System.out.println(" [done]");
      }
      index++;

      // if (index < 20)
      // continue;

      FileInputStream fin = new FileInputStream(jpg.toFile());
      BufferedImage image = javax.imageio.ImageIO.read(fin);
      fin.close();
      writer.writeFrame(os, image);

      System.out.println(" Added " + jpg.toFile().length() + " bytes.");
    }

    writer.finishWrite(os);
    os.close();
  }

}
