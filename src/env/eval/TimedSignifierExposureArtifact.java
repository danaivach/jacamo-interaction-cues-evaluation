package eval;

import cartago.events.ArtifactObsEvent;
import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphReader;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.ResourceProfile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hyperagents.jacamo.artifacts.hmas.SignifierExposureArtifact;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class TimedSignifierExposureArtifact extends SignifierExposureArtifact {

  private  String fileName;
  private Workbook workbook;
  private Sheet sheet;
  public Map<Integer, Long> exposureTimes;
  int counter;

  public void init() {
    super.init();

    this.counter = 0;
    this.fileName =  "sem_time.xlsx";
    this.exposureTimes = new HashMap<Integer, Long>();

    if (Files.exists(Paths.get(fileName))) {
      try (FileInputStream fis = new FileInputStream(fileName)) {
        workbook = new XSSFWorkbook(fis);
        sheet = workbook.getSheet("Log");
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      workbook = new XSSFWorkbook();
      sheet = workbook.createSheet("Log");
      Row headerRow = sheet.createRow(0);
      Cell headerCell1 = headerRow.createCell(0);
      headerCell1.setCellValue("signifiers_num");
      Cell headerCell2 = headerRow.createCell(1);
      headerCell2.setCellValue("sem_time_ms");
    }
  }




  @Override
  protected SignifierFilter getSignifierFilter(String agentProfileUrl) {
    ResourceProfile agentProfile = getAgentProfile();
    return new TimedSignifierFilter(this, agentProfile);
  }

  // Method to log time in the Excel sheet
  private void logInMap(long elapsedTimeInMillis) {

    this.exposureTimes.put(this.counter, elapsedTimeInMillis);

    if (this.counter == 2000) {
      // Start writing map contents to the sheet
      int rowNum = sheet.getLastRowNum() + 1; // Find the next empty row

      // Iterate through the map and write key-value pairs to the sheet
      for (Map.Entry<Integer, Long> entry : this.exposureTimes.entrySet()) {
        Row row = sheet.createRow(rowNum++); // Create a new row and increment rowNum

        // Create cell for the key (in column 0)
        Cell cell1 = row.createCell(0);
        cell1.setCellValue(entry.getKey());

        // Create cell for the value (in column 1)
        Cell cell2 = row.createCell(1);
        cell2.setCellValue(entry.getValue());
      }

      // Write the output to the Excel file
      try (FileOutputStream fos = new FileOutputStream(fileName)) {
        workbook.write(fos);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    // Increment the counter
    this.counter++;
  }

  // Method to log time in the Excel sheet
  private void logInFile(long elapsedTimeInMillis) {

    if (this.counter > 0) {
      // Find the next empty row

      int rowNum = sheet.getLastRowNum() + 1;
      Row row = sheet.createRow(rowNum);

      // Write data to the row
      Cell cell1 = row.createCell(0);
      cell1.setCellValue(rowNum);

      Cell cell2 = row.createCell(1);
      cell2.setCellValue(elapsedTimeInMillis);

      // Write the output to the Excel file
      try (FileOutputStream fos = new FileOutputStream(fileName)) {
        workbook.write(fos);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    counter++;
  }

  private ResourceProfile getAgentProfile() {

    String agentProfileStr = "@prefix jacamo: <https://purl.org/hmas/jacamo/> .\n" +
            "@prefix ex: <https://example.org/> .\n" +
            "@prefix hctl: <https://www.w3.org/2019/wot/hypermedia#> .\n" +
            "@prefix htv: <http://www.w3.org/2011/http#> .\n" +
            "@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
            "@prefix prov: <http://www.w3.org/ns/prov#> .\n" +
            "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
            "@prefix xs: <http://www.w3.org/2001/XMLSchema#> .\n" +
            "@prefix hmas: <https://purl.org/hmas/> .\n" +
            "\n" +
            "<https://wiser-solid-xi.interactions.ics.unisg.ch/agent0/profile/card> a hmas:ResourceProfile;\n" +
            "  hmas:isProfileOf <https://wiser-solid-xi.interactions.ics.unisg.ch/agent0/profile/card#me> .\n" +
            "\n" +
            "<https://wiser-solid-xi.interactions.ics.unisg.ch/agent0/profile/card#me> a hmas:Agent;\n" +
            "  hmas:hasAbility [ a hmas:Ability, ex:Pyromancy0\n" +
            "    ];\n" +
            "  jacamo:hasBody <http://172.27.52.55:8080/workspaces/61/artifacts/agent0#artifact> .\n";
    return ResourceProfileGraphReader.readFromString(agentProfileStr);
  }

  class TimedSignifierFilter extends SignifierFilter {

    public TimedSignifierFilter(TimedSignifierExposureArtifact artifact, ResourceProfile agentProfile) {
      super(artifact, agentProfile);
    }

    @Override
    public boolean select(ArtifactObsEvent ev) {
      long startTime = System.currentTimeMillis();
      boolean selected = super.select(ev);
      long endTime = System.currentTimeMillis();
      ((TimedSignifierExposureArtifact) artifact).logInMap(endTime-startTime);
      return selected;
    }
  }
}
