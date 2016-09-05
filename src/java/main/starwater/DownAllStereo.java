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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
    Date date = Calendar.getInstance().getTime();

    SimpleDateFormat uriformat = new SimpleDateFormat("yyyy/MM/dd");
    SimpleDateFormat fileformat = new SimpleDateFormat("yyyy-MM-dd");

    URL website =
        new URL(
            "http://stereo.gsfc.nasa.gov/browse/" + uriformat.format(date) + "/ahead/cor2/512/");
    try (InputStream in = website.openStream()) {
      Files.copy(in, Paths.get("512.html"), StandardCopyOption.REPLACE_EXISTING);
    }
    List<String> urls =
        extractUrlsFromString(String.join("\n", Files.readAllLines(Paths.get("512.html"))));

    System.out.println("Found " + urls.size() + " images linked in " + website);
    Path target = Paths.get("images", fileformat.format(date));
    Files.createDirectories(target);

    AnimatedGIFWriter writer = new AnimatedGIFWriter(true);
    OutputStream os = new FileOutputStream(fileformat.format(date) + "-ahead-cor2-512.gif");
    writer.prepareForWrite(os, -1, -1);

    for (String spec : urls) {
      URL url = new URL(website, spec);
      Path jpg = target.resolve(spec);
      System.out.println(spec + " -> " + url);
      if (!jpg.toFile().exists()) {
        System.out.println(" already downloaded.");
        try (InputStream in = url.openStream()) {
          Files.copy(in, jpg, StandardCopyOption.REPLACE_EXISTING);
        }
      }

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
