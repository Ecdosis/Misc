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
import calliope.core.database.Connector;
import calliope.core.database.Connection;
import calliope.core.constants.JSONKeys;
import calliope.core.handler.GetHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import misc.exception.MiscException;
import misc.constants.Params;
import java.util.HashSet;
import java.util.Arrays;
import org.json.simple.JSONArray;

/**
 *
 * @author desmond
 */
public class MiscCategoriesHandler extends MiscGetHandler {
    String docid;
    String shortenDocid( String fullDocid )
    {
        int index = fullDocid.lastIndexOf("/");
        if ( index != -1 )
            fullDocid = fullDocid.substring(0,index);
        index = fullDocid.indexOf(docid);
        if ( index != -1 && index+docid.length()+1 < fullDocid.length() )
            fullDocid = fullDocid.substring(index+docid.length()+1);
        else
            fullDocid = "";
        return fullDocid;
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
        docid = request.getParameter(Params.DOCID);
        if ( docid != null && docid.length()>0 )
        {
            try
            {
                Connection conn = Connector.getConnection();
                String[] docids = conn.listDocuments( Database.MISC, 
                    docid+"/.*", JSONKeys.DOCID );
                HashSet<String> categories = new HashSet<String>();
                for ( int i=0;i<docids.length;i++ )
                {
                    String shortID = shortenDocid( docids[i]);
                    if ( !categories.contains(shortID) && shortID.length()> 0 )
                        categories.add( shortID );
                }
                String[] cats = new String[categories.size()];
                categories.toArray(cats);
                Arrays.sort(cats);
                JSONArray jArray = new JSONArray();
                for ( int i=0;i<cats.length;i++ )
                {
                    jArray.add(cats[i]);
                }
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/json");
                response.getOutputStream().println(strip(jArray.toJSONString())); 
            }
            catch ( Exception e )
            {
                throw new MiscException(e);
            }
        }
        else
            throw new MiscException("Missing docid");
    }
}
