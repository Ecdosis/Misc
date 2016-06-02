/*
 * This file is part of Misc.
 *
 *  Misc is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Misc is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Misc.  If not, see <http://www.gnu.org/licenses/>.
 *  (c) copyright Desmond Schmidt 2015
 */

package misc;
import javax.servlet.*;
import java.io.IOException;

/**
 * We need a filter to enforce to UTF-8 for direct submissions via parameters
 * @author desmond
 */
public class EncodingFilter implements Filter {

    private String encoding = "utf-8";

    public void doFilter(ServletRequest request, ServletResponse response, 
        FilterChain filterChain) throws IOException, ServletException 
    {
        request.setCharacterEncoding(encoding);
        filterChain.doFilter(request, response);
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        String encodingParam = filterConfig.getInitParameter("encoding");
        if (encodingParam != null) 
            encoding = encodingParam;
    }

    public void destroy() 
    {
        // nothing todo
    }
}