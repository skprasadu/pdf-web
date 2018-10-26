package technology.tabula.tabula_web.routes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.sourceforge.tess4j.TesseractException;
import spark.Request;
import spark.Response;
import spark.Route;
import technology.tabula.Cell;
import technology.tabula.ObjectExtractor;
import technology.tabula.RectangularTextContainer;
import technology.tabula.TextChunk;
import technology.tabula.json.TextChunkSerializer;
import technology.tabula.tabula_web.extractor.CoordSpec;
import technology.tabula.tabula_web.extractor.Extractor;
import technology.tabula.tabula_web.extractor.TableWithSpecIndex;
import technology.tabula.tabula_web.extractor.TableWithSpecIndexSerializer;
import technology.tabula.tabula_web.workspace.WorkspaceDAO;
import technology.tabula.writers.CSVWriter;
import technology.tabula.writers.TSVWriter;

public class ExtractDataRoute implements Route {

    static class TableSerializerExclusionStrategy implements ExclusionStrategy {

        @Override
        public boolean shouldSkipClass(Class<?> arg0) {
            return false;
        }

        @Override
        public boolean shouldSkipField(FieldAttributes fa) {
            return !fa.hasModifier(Modifier.PUBLIC);
        }
    }

    private WorkspaceDAO workspaceDAO;

    public ExtractDataRoute(WorkspaceDAO workspaceDAO) {
        this.workspaceDAO = workspaceDAO;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        String requestedCoords = request.queryParams("coords");
        Type targetClassType = new TypeToken<ArrayList<CoordSpec>>() {
        }.getType();
        ArrayList<CoordSpec> specs = new Gson().fromJson(requestedCoords, targetClassType);

        // sort extraction specs by page number, then vertical position and then horizontal position
        Collections.sort(specs);

        int i = 0;
        for (CoordSpec spec : specs) {
            spec.spec_index = i++;
        }

        //List<TableWithSpecIndex> tables = Extractor.extractTables(this.workspaceDAO.getDocumentPath(request.params(":file_id")), specs);

        // which format?
        //String requestedFormat = request.queryParams(":format");
        String requestedFormat = request.queryParams("format");
        System.out.println("requestedFormat=" + requestedFormat);
        if (requestedFormat == null) requestedFormat = "json";

        // custom filename?
        String filename = "tabula-" + request.params(":file_id");
        if (request.params("new_filename") != null && request.params("new_filename").trim().length() > 0) {
            filename = "tabula-" + FileSystems.getDefault().getPath(request.params("new_filename")).getFileName().toString();
        }
        
        //String templateModelJson = request.params("template_model_json");
        String templateModelJson = request.queryParams("template_model_json");
        String fileName = this.workspaceDAO.getDocumentPath(request.params(":file_id"));
        System.out.println("templateModelJson=" + templateModelJson);
        System.out.println("fileName=" + fileName);
        
        File file = new File(fileName);
        try(PDDocument doc = PDDocument.load(file)){
	
	        StringBuilder sb;
	        switch (requestedFormat) {
	
	            case "csv":
	                /*for (TableWithSpecIndex t : tables) {
	                    new CSVWriter().write(sb, t);
	                }*/
	
	            	System.out.println("CSV");
	                response.type("text/csv");
	                response.header("Content-Disposition", "attachment; filename=\"tabula" + filename + ".csv\"");
	                return getCsvData(doc, templateModelJson);
	
	            case "tsv":
	                sb = new StringBuilder();
	                /*for (TableWithSpecIndex t : tables) {
	                    new TSVWriter().write(sb, t);
	                }
	
	                response.type("text/csv");
	                response.header("Content-Disposition", "attachment; filename=\"tabula" + filename + ".tsv\"");*/
	                return sb.toString();
	
	            case "zip":
	                /*ByteArrayOutputStream baos = new ByteArrayOutputStream();
	                ZipOutputStream zos = new ZipOutputStream(baos);
	                int idx = 0;
	
	                for (TableWithSpecIndex t : tables) {
	                    ZipEntry entry = new ZipEntry(filename + "-" + (idx++) + ".csv");
	                    zos.putNextEntry(entry);
	
	                    sb = new StringBuilder();
	                    new CSVWriter().write(sb, t);
	                    zos.write(sb.toString().getBytes("UTF-8"));
	                    zos.closeEntry();
	                }
	                zos.finish();
	
	                response.header("Content-Disposition", "attachment; filename=\"tabula" + filename + ".zip\"");
	                return baos.toByteArray();*/
	            	return "";
	
	            case "json":
	                /*Gson gson = new GsonBuilder()
	                        .addSerializationExclusionStrategy(new TableSerializerExclusionStrategy())
	                        .registerTypeAdapter(TableWithSpecIndex.class, new TableWithSpecIndexSerializer())
	                        .registerTypeAdapter(RectangularTextContainer.class, new TextChunkSerializer())
	                        .registerTypeAdapter(Cell.class, new TextChunkSerializer())
	                        .registerTypeAdapter(TextChunk.class, new TextChunkSerializer())
	                        .create();*/
	
	                response.type("application/json");
	                return getJsonData(doc, templateModelJson);
	
	            case "bbox":
	                response.type("application/json");
	                response.header("Content-Disposition", "attachment; filename=\"" + filename + "json\"");
	                return new GsonBuilder().create().toJson(specs);
	        }
        
        }

        return "";

    }

	private String getJsonData(PDDocument doc, String templateModelJson) throws Exception {
        ObjectExtractor o = new ObjectExtractor(doc);
        String out = o.extractJson(templateModelJson);

		return out;
	}

	private String getCsvData(PDDocument doc, String templateModelJson) throws Exception {
        ObjectExtractor o = new ObjectExtractor(doc);
        String out = o.extractCsv(templateModelJson);

		return out;
	}

}
