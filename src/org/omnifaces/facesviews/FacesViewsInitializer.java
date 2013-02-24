package org.omnifaces.facesviews;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static javax.faces.view.facelets.ResourceResolver.FACELETS_RESOURCE_RESOLVER_PARAM_NAME;
import static org.omnifaces.facesviews.FacesViews.FACES_VIEWS_ENABLED_PARAM_NAME;
import static org.omnifaces.facesviews.FacesViews.FACES_VIEWS_RESOURCES;
import static org.omnifaces.facesviews.FacesViews.FACES_VIEWS_RESOURCES_EXTENSIONS;
import static org.omnifaces.facesviews.FacesViews.scanViewsFromRootPaths;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * Convenience class for Servlet 3.0 users, which will auto-register most artifacts
 * required for auto-mapping and extensionless view to work.
 * <p>
 * For a guide on FacesViews, please see the <a href="package-summary.html">package summary</a>.
 *
 * @author Arjan Tijms
 *
 */
public class FacesViewsInitializer implements ServletContainerInitializer {

	@Override
	public void onStartup(Set<Class<?>> c, ServletContext servletContext) throws ServletException {

		if (!"false".equals(servletContext.getInitParameter(FACES_VIEWS_ENABLED_PARAM_NAME))) {

			// Scan our dedicated directory for Faces resources that need to be mapped
			Map<String, String> collectedViews = new HashMap<String, String>();
			Set<String> collectedExtentions = new HashSet<String>();
			scanViewsFromRootPaths(servletContext, collectedViews, collectedExtentions);

			if (!collectedViews.isEmpty()) {

				// Store the resources and extensions that were found in application scope, where others can find it.
				servletContext.setAttribute(FACES_VIEWS_RESOURCES, unmodifiableMap(collectedViews));
				servletContext.setAttribute(FACES_VIEWS_RESOURCES_EXTENSIONS, unmodifiableSet(collectedExtentions));

				// Register 3 artifacts with the Servlet container and JSF that help implement this feature:

				// 1. A Filter that forwards extensionless requests to an extension mapped request, e.g. /index to
				// /index.xhtml
				// (The FacesServlet doesn't work well with the exact mapping that we use for extensionless URLs).
				FilterRegistration facesViewsRegistration = servletContext.addFilter(FacesViewsForwardingFilter.class.getName(),
						FacesViewsForwardingFilter.class);

				// 2. A Facelets resource resolver that resolves requests like /index.xhtml to
				// /WEB-INF/faces-views/index.xhtml
				servletContext.setInitParameter(FACELETS_RESOURCE_RESOLVER_PARAM_NAME, FacesViewsResolver.class.getName());

				// 3. A ViewHandler that transforms the forwarded extension based URL back to an extensionless one, e.g.
				// /index.xhtml to /index
				// See FacesViewsForwardingFilter#init

				// Map the forwarding filter to all the resources we found.
				for (String resource : collectedViews.keySet()) {
					facesViewsRegistration.addMappingForUrlPatterns(null, false, resource);
				}
				
				// We now need to map the Faces Servlet to the extensions we found, but at this point in time
				// this Faces Servlet might not be created yet, so we do this part in FacesViewInitializedListener.
			}
		}
	}

}