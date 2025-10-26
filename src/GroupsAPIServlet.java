import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * REST API Servlet for study groups Provides endpoints to view and manage study
 * groups
 */
@WebServlet(name = "GroupsAPIServlet", urlPatterns = { "/api/groups/*" })
public class GroupsAPIServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		String pathInfo = request.getPathInfo();

		try {
			if (pathInfo != null && pathInfo.startsWith("/user/")) {
				// Get groups for a specific user
				String userIdStr = pathInfo.substring(6); // Remove "/user/"
				int userId = Integer.parseInt(userIdStr);
				getUserGroups(userId, response);
			} else if ("/all".equals(pathInfo)) {
				// Get all groups
				getAllGroups(response);
			} else {
				sendErrorResponse(response, 404, "Endpoint not found");
			}
		} catch (NumberFormatException e) {
			sendErrorResponse(response, 400, "Invalid user ID");
		} catch (SQLException e) {
			e.printStackTrace();
			sendErrorResponse(response, 500, "Database error occurred");
		}
	}

	/**
	 * Get all study groups for a specific user
	 */
	private void getUserGroups(int userId, HttpServletResponse response) throws SQLException, IOException {

		Connection connection = (Connection) getServletContext().getAttribute("dbConnection");

		String sql = "SELECT sg.*, sgm.membership_id " + "FROM Study_groups sg "
				+ "INNER JOIN Study_group_membership sgm ON sg.group_id = sgm.group_id " + "WHERE sgm.user_id = ? "
				+ "ORDER BY sg.group_name";

		List<String> groups = new ArrayList<>();

		try (PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setInt(1, userId);

			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					groups.add(resultSetToJson(rs));
				}
			}
		}

		// Build JSON response
		StringBuilder json = new StringBuilder();
		json.append("{\"success\":true,\"groups\":[");

		for (int i = 0; i < groups.size(); i++) {
			json.append(groups.get(i));
			if (i < groups.size() - 1) {
				json.append(",");
			}
		}

		json.append("]}");

		PrintWriter out = response.getWriter();
		out.print(json.toString());
		out.flush();
	}

	/**
	 * Get all study groups (for browsing/joining)
	 */
	private void getAllGroups(HttpServletResponse response) throws SQLException, IOException {

		Connection connection = (Connection) getServletContext().getAttribute("dbConnection");

		String sql = "SELECT * FROM Study_groups ORDER BY group_name";

		List<String> groups = new ArrayList<>();

		try (PreparedStatement stmt = connection.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {

			while (rs.next()) {
				groups.add(resultSetToJson(rs));
			}
		}

		// Build JSON response
		StringBuilder json = new StringBuilder();
		json.append("{\"success\":true,\"groups\":[");

		for (int i = 0; i < groups.size(); i++) {
			json.append(groups.get(i));
			if (i < groups.size() - 1) {
				json.append(",");
			}
		}

		json.append("]}");

		PrintWriter out = response.getWriter();
		out.print(json.toString());
		out.flush();
	}

	/**
	 * Convert ResultSet row to JSON string
	 */
	private String resultSetToJson(ResultSet rs) throws SQLException {
		StringBuilder json = new StringBuilder();
		json.append("{");
		json.append("\"group_id\":").append(rs.getInt("group_id")).append(",");
		json.append("\"group_name\":\"").append(escapeJson(rs.getString("group_name"))).append("\",");
		json.append("\"class_name\":\"").append(escapeJson(rs.getString("class_name"))).append("\",");
		json.append("\"class_code\":\"").append(escapeJson(rs.getString("class_code"))).append("\",");
		json.append("\"subject\":\"").append(escapeJson(rs.getString("subject"))).append("\"");

		// Optional fields
		String description = rs.getString("descripton");
		if (description != null && !description.isEmpty()) {
			json.append(",\"descripton\":\"").append(escapeJson(description)).append("\"");
		}

		String location = rs.getString("meeting_location");
		if (location != null && !location.isEmpty()) {
			json.append(",\"meeting_location\":\"").append(escapeJson(location)).append("\"");
		}

		Time meetingTime = rs.getTime("meeting_time");
		if (meetingTime != null) {
			json.append(",\"meeting_time\":\"").append(meetingTime.toString()).append("\"");
		}

		String days = rs.getString("meeting_days");
		if (days != null && !days.isEmpty()) {
			json.append(",\"meeting_days\":\"").append(escapeJson(days)).append("\"");
		}

		json.append("}");
		return json.toString();
	}

	/**
	 * Escape special characters in JSON strings
	 */
	private String escapeJson(String text) {
		if (text == null) {
			return "";
		}
		return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t",
				"\\t");
	}

	/**
	 * Send error response
	 */
	private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
		response.setStatus(status);
		PrintWriter out = response.getWriter();

		StringBuilder json = new StringBuilder();
		json.append("{");
		json.append("\"success\":false,");
		json.append("\"message\":\"").append(escapeJson(message)).append("\"");
		json.append("}");

		out.print(json.toString());
		out.flush();
	}
}