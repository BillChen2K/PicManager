import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.util.List;

import bean.Asset;
import utils.Config;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import utils.DatabaseManager;
import utils.Utils;

@WebServlet(name = "uploadfile", urlPatterns = "/manage/uploadfile")
public class UploadFile extends HttpServlet {
	private DatabaseManager db = new DatabaseManager();
	private static final long serialVersionUID = 12431L;

	/**
	 * 上传数据及保存文件
	 */
	protected void doPost(HttpServletRequest request,
						  HttpServletResponse response) throws ServletException, IOException {
		db.getConnection();

		Asset newAsset = new Asset();
		HttpSession session = request.getSession();
		// 检测是否为多媒体上传
		// 跳转到 message.jsp
		PrintWriter out = response.getWriter();
		if (!ServletFileUpload.isMultipartContent(request)) {
			// 如果不是则停止
			PrintWriter writer = response.getWriter();
			writer.println("Error: 表单必须包含 enctype=multipart/form-data");
			writer.flush();
			return;
		}
		// 配置上传参数
		DiskFileItemFactory factory = new DiskFileItemFactory();
		// 设置内存临界值 - 超过后将产生临时文件并存储于临时目录中
		factory.setSizeThreshold(Config.MEMORY_THRESHOLD);
		// 设置临时存储目录
		factory.setRepository(new File(System.getProperty("java.io.tmpdir")));
		ServletFileUpload upload = new ServletFileUpload(factory);
		// 设置最大文件上传值
		upload.setFileSizeMax(Config.MAX_FILE_SIZE);
		// 设置最大请求值 (包含文件和表单数据)
		upload.setSizeMax(Config.MAX_REQUEST_SIZE);
		// 中文处理
		//upload.setHeaderEncoding("UTF-8");
		upload.setHeaderEncoding("ISO8859_1");
		// 构造临时路径来存储上传的文件
		// 这个路径相对当前应用的目录
		String uploadPath = getServletContext().getRealPath("/") + File.separator + Config.UPLOAD_DIRECTORY;
		// 如果目录不存在则创建
		File uploadDir = new File(uploadPath);
		if (!uploadDir.exists()) {
			uploadDir.mkdir();
		}
		System.out.println("---" + request.getParameter("uploadFile"));
		System.out.println(request.getParameter("uploadScale"));
		String newAssetName = "";
		Integer newAssetID = -1;
		try {
			// 解析请求的内容提取文件数据
			@SuppressWarnings("unchecked")
			List<FileItem> formItems = upload.parseRequest(request);
			if (formItems != null && formItems.size() > 0) {
				// 迭代表单数据
				for (FileItem item : formItems) {
					// 处理不在表单中的字段
					if (!item.isFormField()) {
						String fileName = new File(item.getName()).getName();
						String filePath = uploadPath + File.separator + fileName;
						File storeFile = new File(filePath);
						// 在控制台输出文件的上传路径
						System.out.println(filePath);
						// 保存文件到硬盘
						item.write(storeFile);
						newAsset.setUrl(Config.UPLOAD_DIRECTORY + fileName);
					}
					else{

						switch (item.getFieldName()){
							case "uploadName":
								newAssetName = new String(item.getString().getBytes("ISO8859_1"), "utf-8");
								newAsset.setName(newAssetName);
								break;
							case "uploadCountry":
								newAsset.setCountry(new String(item.getString().getBytes("ISO8859_1"), "utf-8"));
								break;
							case "uploadLocation":
								newAsset.setLocation(new String(item.getString().getBytes("ISO8859_1"), "utf-8"));
								break;
							case "uploadLatitude":
								newAsset.setLatitude(new String(item.getString().getBytes("ISO8859_1"), "utf-8"));
								break;
							case "uploadLongitude":
								newAsset.setLongitude(new String(item.getString().getBytes("ISO8859_1"), "utf-8"));
								break;
							case "uploadScale":
								newAsset.setScale(new String(item.getString().getBytes("ISO8859_1"), "utf-8"));
								break;
							case "uploadCategory":
								newAsset.setCategory(new String(item.getString().getBytes("ISO8859_1"), "utf-8"));
								break;
						}
						System.out.println(item.getFieldName() + ": " + new String(item.getString().getBytes("ISO8859_1"), "utf-8"));
					}
				}
				String sqlQ = "INSERT INTO asset (assetname, assettype, category, url, country, location, latitude, longitude, acq_date, scale, upload_date, last_modified_date, uploader_uid) VALUES" +
						"('" + newAsset.getName() + "', '" + "pic" + "', '" + newAsset.getCategory() + "', '" + newAsset.getUrl() + "', '" + newAsset.getCountry() + "', '" + newAsset.getLocation() + "', '" + newAsset.getLatitude() + "', '" + newAsset.getLongitude() + "', '" +
						Utils.getCurrentDateTime() + "', '" + newAsset.getScale() + "', '" + Utils.getCurrentDateTime() + "', '" + Utils.getCurrentDateTime() + "', '" + session.getAttribute("logined_uid") + "');";
				db.executeUpdate(sqlQ);
				ResultSet rs = db.executeQuery("SELECT `assetid` FROM `asset` ORDER BY `assetid` DESC;");
				rs.next();
				newAssetID = rs.getInt("assetid");
				String logSQL = "INSERT INTO `picmanager`.`log`(`uid`, `username`,  `assetid`, `assetname`, `type`, `date`, `request_ip`, `notes`) VALUES ('" + session.getAttribute("logined_uid") + "', '"
						+ session.getAttribute("logined_username") + "', '" + newAssetID.toString() + "', '" + newAssetName + "', 'upload', '" + Utils.getCurrentDateTime() + "', '" + Utils.getRealRemoteIP(request) + "', NULL);";

				db.executeUpdate(logSQL);
				session.setAttribute("upload_stat","success");
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			session.setAttribute("upload_stat",
								 "Upload Fail:" + ex.getStackTrace());
			response.sendRedirect("/manage");
			return;
		}
		response.sendRedirect("/manage");
		return;
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}
}
