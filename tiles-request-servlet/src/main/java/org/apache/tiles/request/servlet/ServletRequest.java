/*
 * $Id$
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tiles.request.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tiles.request.AbstractClientRequest;
import org.apache.tiles.request.ApplicationContext;
import org.apache.tiles.request.collection.AddOnlyMap;
import org.apache.tiles.request.collection.HeaderValuesMap;
import org.apache.tiles.request.collection.ReadOnlyEnumerationMap;
import org.apache.tiles.request.collection.ScopeMap;
import org.apache.tiles.request.servlet.extractor.ParameterExtractor;
import org.apache.tiles.request.servlet.extractor.RequestScopeExtractor;
import org.apache.tiles.request.servlet.extractor.HeaderExtractor;
import org.apache.tiles.request.servlet.extractor.SessionScopeExtractor;

/**
 * Servlet-based implementation of the TilesApplicationContext interface.
 *
 * @version $Rev$ $Date$
 */
public class ServletRequest extends AbstractClientRequest {

    /**
     * The native available scopes: request, session and application.
     */
    private static final String[] SCOPES = {"request", "session", "application"};

    /**
     * The request object to use.
     */
    private HttpServletRequest request;

    /**
     * The response object to use.
     */
    private HttpServletResponse response;

    /**
     * The response output stream, lazily initialized.
     */
    private OutputStream outputStream;

    /**
     * The response writer, lazily initialized.
     */
    private PrintWriter writer;

    /**
     * <p>The lazily instantiated <code>Map</code> of header name-value
     * combinations (immutable).</p>
     */
    private Map<String, String> header = null;

    /**
     * <p>The lazily instantiated <code>Map</code> of header name-value
     * combinations (write-only).</p>
     */
    private Map<String, String> responseHeaders = null;


    /**
     * <p>The lazily instantitated <code>Map</code> of header name-values
     * combinations (immutable).</p>
     */
    private Map<String, String[]> headerValues = null;


    /**
     * <p>The lazily instantiated <code>Map</code> of request
     * parameter name-value.</p>
     */
    private Map<String, String> param = null;


    /**
     * <p>The lazily instantiated <code>Map</code> of request scope
     * attributes.</p>
     */
    private Map<String, Object> requestScope = null;

    /**
     * <p>The lazily instantiated <code>Map</code> of session scope
     * attributes.</p>
     */
    private Map<String, Object> sessionScope = null;


    /**
     * Creates a new instance of ServletTilesRequestContext.
     *
     * @param applicationContext The application context.
     * @param request The request object.
     * @param response The response object.
     * @since 2.1.1
     */
    public ServletRequest(
            ApplicationContext applicationContext,
            HttpServletRequest request, HttpServletResponse response) {
        super(applicationContext);
        this.request = request;
        this.response = response;
    }

    /** {@inheritDoc} */
    public Map<String, String> getHeader() {

        if ((header == null) && (request != null)) {
            header = new ReadOnlyEnumerationMap<String>(new HeaderExtractor(request, null));
        }
        return (header);

    }

    /** {@inheritDoc} */
    public Map<String, String> getResponseHeaders() {

        if ((responseHeaders == null) && (response != null)) {
            responseHeaders = new AddOnlyMap<String>(new HeaderExtractor(null, response));
        }
        return (responseHeaders);

    }

    /** {@inheritDoc} */
    public Map<String, String[]> getHeaderValues() {

        if ((headerValues == null) && (request != null)) {
            headerValues = new HeaderValuesMap(new HeaderExtractor(request, response));
        }
        return (headerValues);

    }


    /** {@inheritDoc} */
    public Map<String, String> getParam() {

        if ((param == null) && (request != null)) {
            param = new ReadOnlyEnumerationMap<String>(new ParameterExtractor(request));
        }
        return (param);

    }


    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public Map<String, String[]> getParamValues() {
        return request.getParameterMap();
    }


    /** {@inheritDoc} */
    public Map<String, Object> getRequestScope() {

        if ((requestScope == null) && (request != null)) {
            requestScope = new ScopeMap(new RequestScopeExtractor(request));
        }
        return (requestScope);

    }


    /** {@inheritDoc} */
    public Map<String, Object> getSessionScope() {

        if ((sessionScope == null) && (request != null)) {
            sessionScope = new ScopeMap(new SessionScopeExtractor(request));
        }
        return (sessionScope);

    }

    @Override
    public String[] getNativeScopes() {
        return SCOPES;
    }

    /** {@inheritDoc} */
    public void doForward(String path) throws IOException {
        if (response.isCommitted()) {
            doInclude(path);
        } else {
            forward(path);
        }
    }


    /** {@inheritDoc} */
    public void doInclude(String path) throws IOException {
        RequestDispatcher rd = request.getRequestDispatcher(path);

        if (rd == null) {
            throw new IOException("No request dispatcher returned for path '"
                    + path + "'");
        }

        try {
            rd.include(request, response);
        } catch (ServletException ex) {
            throw ServletUtil.wrapServletException(ex, "ServletException including path '"
                    + path + "'.");
        }
    }

    /**
     * Forwards to a path.
     *
     * @param path The path to forward to.
     * @throws IOException If something goes wrong during the operation.
     */
    private void forward(String path) throws IOException {
        RequestDispatcher rd = request.getRequestDispatcher(path);

        if (rd == null) {
            throw new IOException("No request dispatcher returned for path '"
                    + path + "'");
        }

        try {
            rd.forward(request, response);
        } catch (ServletException ex) {
            throw ServletUtil.wrapServletException(ex, "ServletException including path '"
                    + path + "'.");
        }
    }

    /** {@inheritDoc} */
    public OutputStream getOutputStream() throws IOException {
        if (outputStream == null) {
            outputStream = response.getOutputStream();
        }
        return outputStream;
    }

    /** {@inheritDoc} */
    public Writer getWriter() throws IOException {
        return getPrintWriter();
    }

    /** {@inheritDoc} */
    public PrintWriter getPrintWriter() throws IOException {
        if (writer == null) {
            writer = response.getWriter();
        }
        return writer;
    }

    /** {@inheritDoc} */
    public boolean isResponseCommitted() {
        return response.isCommitted();
    }

    /** {@inheritDoc} */
    public void setContentType(String contentType) {
        response.setContentType(contentType);
    }

    /** {@inheritDoc} */
    public Locale getRequestLocale() {
        return request.getLocale();
    }

    /** {@inheritDoc} */
    public HttpServletRequest getRequest() {
        return request;
    }

    /** {@inheritDoc} */
    public HttpServletResponse getResponse() {
        return response;
    }

    /** {@inheritDoc} */
    public boolean isUserInRole(String role) {
        return request.isUserInRole(role);
    }
}
