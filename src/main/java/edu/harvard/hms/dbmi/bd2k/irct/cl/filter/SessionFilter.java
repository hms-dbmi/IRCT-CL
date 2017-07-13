/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package edu.harvard.hms.dbmi.bd2k.irct.cl.filter;

import java.io.IOException;
import java.security.SignatureException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
//import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import edu.harvard.hms.dbmi.bd2k.irct.controller.SecurityController;
import edu.harvard.hms.dbmi.bd2k.irct.model.security.JWT;
import edu.harvard.hms.dbmi.bd2k.irct.model.security.SecureSession;
import edu.harvard.hms.dbmi.bd2k.irct.model.security.Token;
import edu.harvard.hms.dbmi.bd2k.irct.model.security.User;

/**
 * Creates a session filter for ensuring secure access
 *
 * @author Jeremy R. Easton-Marks
 *
 */
@WebFilter(filterName = "session-filter", urlPatterns = { "/*" })
public class SessionFilter implements Filter {

	Logger logger = Logger.getLogger(this.getClass().getName());

	@javax.annotation.Resource(mappedName = "java:global/client_id")
	private String clientId;
	@javax.annotation.Resource(mappedName = "java:global/client_secret")
	private String clientSecret;
	@javax.annotation.Resource(mappedName = "java:global/userField")
	private String userField;

	@Inject
	private SecurityController sc;

	@Override
	public void init(FilterConfig fliterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain fc) throws IOException, ServletException {
		logger.log(Level.FINE, "doFilter() Starting");
		HttpServletRequest request = (HttpServletRequest) req;

		logger.log(Level.INFO, "doFilter() requestURI:" + request.getRequestURI());

		// Calls to the Security Service can go straight through
		if (!request.getRequestURI().substring(request.getContextPath().length()).startsWith("/securityService/")) {
			// Get the session and user information
			HttpSession session = ((HttpServletRequest) req).getSession();
			User user = (User) session.getAttribute("user");

			// Is a user already associated with a session?
			if (user == null) {
				logger.log(Level.SEVERE, "doFilter() Expected user information in session, but it is not there");

				// If no user is associated then validate the authorization
				// header
				String email = validateAuthorizationHeader((HttpServletRequest) req);

				if (email == null) {
					((HttpServletResponse) res).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					res.getOutputStream().write("{\"message\":\"Session is not authorized\"}".getBytes());
					res.getOutputStream().close();
					return;
				}

				user = sc.getUser(email);
				Token token = new JWT(((HttpServletRequest) req).getHeader("Authorization"), "", "Bearer",
						this.clientId);
				SecureSession secureSession = new SecureSession();
				secureSession.setToken(token);
				secureSession.setUser(user);

				session.setAttribute("user", user);
				session.setAttribute("token", token);
				session.setAttribute("secureSession", secureSession);
			}
		} else {
			String name = validateAuthorizationHeader((HttpServletRequest) req);

			if (name != null) {
				HttpSession session = ((HttpServletRequest) req).getSession();
				logger.log(Level.INFO, "doFilter() "+name+" is logging in.");
				User user = sc.getUser(name);
				Token token = new JWT(((HttpServletRequest) req).getHeader("Authorization"), "", "Bearer",
						this.clientId);
				SecureSession secureSession = new SecureSession();
				secureSession.setToken(token);
				secureSession.setUser(user);

				session.setAttribute("user", user);
				session.setAttribute("token", token);
				session.setAttribute("secureSession", secureSession);
			} else {
				((HttpServletResponse) res).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				res.setContentType("application/json");
				res.getOutputStream()
						.write("{\"status\":\"error\",\"message\":\"Error establising identity from request headers.\"}"
								.getBytes());
				res.getOutputStream().close();
				return;
			}
		}
		logger.log(Level.FINE, "doFilter() Finished");
		fc.doFilter(req, res);
	}

	private String validateAuthorizationHeader(HttpServletRequest req) {
		logger.log(Level.FINE, "validateAuthorizationHeader() Starting");
		String authorizationHeader = ((HttpServletRequest) req).getHeader("Authorization");

		if (authorizationHeader != null) {
			logger.log(Level.FINE, "validateAuthorizationHeader() header:" + authorizationHeader);
			try {

				String[] parts = authorizationHeader.split(" ");

				if (parts.length != 2) {
					return null;
				}
				logger.log(Level.INFO, "validateAuthorizationHeader() "+parts[0] + "/" + parts[1]);

				String scheme = parts[0];
				String credentials = parts[1];
				String token = "";

				Pattern pattern = Pattern.compile("^Bearer$", Pattern.CASE_INSENSITIVE);

				if (pattern.matcher(scheme).matches()) {
					token = credentials;
				}
				logger.log(Level.FINE, "validateAuthorizationHeader() token:" + token);

				try {
					logger.log(Level.FINE, "validateAuthorizationHeader()() clientSecret:" + this.clientSecret);

					Algorithm algo = Algorithm.HMAC256(this.clientSecret);
					JWTVerifier verifier = com.auth0.jwt.JWT.require(algo).build();
					DecodedJWT jwt = verifier.verify(token);
					logger.log(Level.FINE, jwt.toString());

					// OK, we can trust this JWT
					logger.log(Level.INFO, "validateAuthorizationHeader() Token trusted)");
					return jwt.getClaim("email").asString();

				} catch (Exception e) {
					logger.log(Level.SEVERE, "validateAuthorizationHeader() Exception:" + e.getMessage());
				}


			} catch (Exception e) {
				// e.printStackTrace();
				logger.log(Level.SEVERE,
						"validateAuthorizationHeader() token validation failed: " + e + "/" + e.getMessage());
			}
		} else {
			logger.log(Level.SEVERE, "validateAuthorizationHeader() Missing 'Authorization' header.");
		}
		logger.log(Level.SEVERE, "validateAuthorizationHeader() Finished (null returned)");
		return null;
	}

	@Override
	public void destroy() {

	}

}
