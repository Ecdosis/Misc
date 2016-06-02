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
 *  (c) copyright Desmond Schmidt 2016
 */

package misc.handler;

import calliope.core.constants.Database;
import calliope.core.constants.Formats;
import calliope.core.handler.GetHandler;
import calliope.core.Utils;
import misc.exception.MiscException;
import calliope.core.handler.EcdosisVersion;
import misc.constants.Params;
import misc.constants.Service;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Get a file in the misc collection. No versions, but maybe links.
 * @author desmond
 */
public class MiscGetHandler extends GetHandler
{
    protected String makeTitle( String docId )
    {
        String[] parts = docId.split("/");
        if ( parts.length>0 )
        {
            String title = parts[parts.length-1].replaceAll("-"," ");
            StringBuilder sb = new StringBuilder();
            int state = 0;
            for ( int i=0;i<title.length();i++ )
            {
                char token = title.charAt(i);
                switch ( state )
                {
                    case 0:
                        if ( token == ' ' )
                        {
                            sb.append(token);
                            state = 1;
                        }
                        else if ( sb.length()==0 && Character.isLetter(token) )
                            sb.append(Character.toUpperCase(token));
                        else
                            sb.append(token);
                        break;
                    case 1:
                        if ( Character.isWhitespace(token) )
                        {
                            sb.append(token);
                        }
                        else 
                        {
                            if ( Character.isLetter(token) )
                                sb.append(Character.toUpperCase(token));
                            else
                                sb.append(token);
                            state = 0;
                        }
                        break;
                }
            }
            return sb.toString();
        }
        else
            return "";
    }
    /**
     * Strip out escaped slashes and replace with ordinary slashes
     * @param str the str to strip
     * @return the stripped string
     */
    protected String strip( String str )
    {
        return str.replaceAll("\\\\/", "/");
    }
    /**
     * Get a miscellaneous paratextual document, image (binary) or text
     * @param request the servlet request
     * @param response the servlet response
     * @param urn the docID, stripped of its prefix
     * @throws MiscException 
     */
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws MiscException
    {
        try
        {
            String first = Utils.first(urn);
            urn = Utils.pop(urn);
            if ( first.equals(Service.HTML) )
                new MiscHTMLGetHandler().handle(request,response,urn);
            else if ( first.equals(Service.CATEGORIES) )
                new MiscCategoriesHandler().handle(request,response,urn);
            else if ( first.equals(Service.DOCUMENTS) )
                new MiscListDocumentsHandler().handle(request,response,urn);
            else if ( first.equals(Service.PREVIOUS) )
                new MiscPreviousHandler().handle(request,response,urn);
            else
            {
                String docID = request.getParameter(Params.DOCID);
                if ( docID != null )
                {
                    EcdosisVersion hv = doGetResourceVersion( 
                        Database.MISC, docID, "" );
                    String contentFormat = hv.getContentFormat().toLowerCase();
                    if ( contentFormat.equals(Formats.TEXT) )
                    {
                        String content = hv.getVersionString();
                        response.setContentType("text/plain");
                        response.getWriter().print(content);
                    }
                    else if ( contentFormat.equals(Formats.HTML) )
                    {
                        String content = hv.getVersionString();
                        response.setContentType("text/html");
                        response.getWriter().print(content);
                    }
                    else if ( contentFormat.equals(Formats.JSON) )
                    {
                        String content = hv.getVersionString();
                        response.setContentType("application/json");
                        response.getWriter().print(content);
                    }
                    else if ( contentFormat.equals(Formats.XML) )
                    {
                        String content = hv.getVersionString();
                        response.setContentType("application/xml");
                        response.getWriter().print(content);
                    }
                    else if ( contentFormat.equals(Formats.MARKDOWN) )
                    {
                        String content = hv.getVersionString();
                        response.setCharacterEncoding("UTF-8");
                        response.setContentType(Formats.MIME_MARKDOWN);
                        response.getWriter().print(content); 
                    }
                    else
                    {
                        char[] data = hv.getVersion();
                        String str = new String( data );
                        response.setCharacterEncoding("UTF-8");
                        response.getOutputStream().write(str.getBytes("UTF-8")); 
                    }
                }
                else
                    throw new Exception("Missing docid param");
            }
        }
        catch ( Exception ioe )
        {
            throw new MiscException( ioe );
        }
    }
}