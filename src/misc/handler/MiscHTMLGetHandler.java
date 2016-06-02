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
import calliope.core.handler.EcdosisVersion;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import misc.constants.Params;
import misc.exception.MiscException;
import org.markdown4j.Markdown4jProcessor;
import java.io.FileNotFoundException;
/**
 * Convert mostly markdown to HTML
 * @author desmond
 */
public class MiscHTMLGetHandler extends MiscGetHandler
{
    
/**
     * Get a document in HTML format
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
            String docid = request.getParameter(Params.DOCID);
            if ( docid != null )
            {
                EcdosisVersion hv = doGetResourceVersion( 
                    Database.MISC, docid, "" );
                if ( hv != null )
                {
                    String contentFormat = hv.getContentFormat().toLowerCase();
                    if ( contentFormat.equals(Formats.MARKDOWN) )
                    {
                        String content = hv.getVersionString();
                        response.setContentType("text/html");
                        response.setCharacterEncoding("UTF-8");
                        String html = new Markdown4jProcessor().process(content);
                        response.getWriter().print(html);
                    }
                    else if ( contentFormat.equals(Formats.HTML) )
                    {
                        String content = hv.getVersionString();
                        response.setCharacterEncoding("UTF-8");
                        response.setContentType("text/html");
                        response.getWriter().print(content);
                    }
                    else
                        throw new Exception("Need markdown or html but received "
                            +contentFormat);
                }
                else
                    throw new FileNotFoundException();
            }
        }
        catch ( Exception e )
        {
            throw new MiscException(e);
        }
    }
}
