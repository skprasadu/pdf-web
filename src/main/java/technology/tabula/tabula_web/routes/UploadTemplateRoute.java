package technology.tabula.tabula_web.routes;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Request;
import spark.Response;
import spark.Route;
import technology.tabula.tabula_web.background.JobExecutor;
import technology.tabula.tabula_web.background.job.GenerateTemplateDocumentData;
import technology.tabula.tabula_web.routes.UploadRoute.UploadStatus;
import technology.tabula.tabula_web.workspace.WorkspaceDAO;

public class UploadTemplateRoute implements Route {

	private WorkspaceDAO workspaceDAO;
	final static Logger logger = LoggerFactory.getLogger(UploadTemplateRoute.class);
	
	public UploadTemplateRoute(WorkspaceDAO workspaceDAO) {
		this.workspaceDAO = workspaceDAO;
	}

	@Override
	public Object handle(Request request, Response response) throws Exception {
		request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(System.getProperty("java.io.tmpdir")));
		
		Part part = request.raw().getPart("files[]");
		String originalFilename = part.getSubmittedFileName();
		String documentId = UUID.randomUUID().toString();
		
		try (InputStream input = part.getInputStream()) {
            workspaceDAO.addTemplateFile(input, originalFilename, "document.json");
		}
		
		UUID jobBatch = UUID.randomUUID();
		JobExecutor executor = JobExecutor.getInstance();

		logger.info("Starting Job Batch {}", jobBatch.toString());

        String documentPath = this.workspaceDAO.getDocumentPath(documentId);
		executor.submitJobs(
				new GenerateTemplateDocumentData(documentPath, originalFilename, documentId,
                        new int[] { 800 }, jobBatch, workspaceDAO)
		);


        ArrayList<UploadStatus> resp = new ArrayList<UploadStatus>();
        //resp.add(new UploadStatus(originalFilename, true, documentId, jobBatch.toString()));

		return resp;
	}

}
