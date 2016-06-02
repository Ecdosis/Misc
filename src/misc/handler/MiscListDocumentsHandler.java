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
import calliope.core.constants.JSONKeys;
import calliope.core.database.Connection;
import calliope.core.database.Connector;
import calliope.core.constants.Formats;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import misc.constants.Params;
import misc.exception.MiscException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 *
 * @author desmond
 */
public class MiscListDocumentsHandler extends MiscGetHandler
{
    String docid;
    String format;
    String shortenDocid( String fullDocid )
    {
        int index = fullDocid.lastIndexOf(docid);
        if ( index != -1 )
            fullDocid = fullDocid.substring(index+docid.length());
        if ( fullDocid.startsWith("/") )
            fullDocid = fullDocid.substring(1);
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
        format = request.getParameter(Params.FORMAT);
        if ( format == null || format.length()==0 )
            format = Formats.MIME_MARKDOWN;
        if ( docid != null && docid.length()>0 )
        {
            try
            {
                Connection conn = Connector.getConnection();
                if ( docid.endsWith("/") )
                    docid = docid.substring(0,docid.length()-1);
                String[] docids = conn.listDocuments( Database.MISC, 
                    docid+"/.*", JSONKeys.DOCID );
                Arrays.sort(docids);
                JSONArray jArray = new JSONArray();
                for ( int i=0;i<docids.length;i++ )
                {
                    String shortID = shortenDocid(docids[i]);
                    // only allow documents at this level
                    if ( !shortID.contains("/") )
                    {
                        String jStr = conn.getFromDb(Database.MISC,docids[i]);
                        JSONObject jObj = (JSONObject)JSONValue.parse(jStr);
                        if ( ((String)jObj.get(JSONKeys.FORMAT)).equals(format) )
                        {
                            if ( !jObj.containsKey(JSONKeys.TITLE) )
                                jObj.put(JSONKeys.TITLE, makeTitle(docids[i]));
                            JSONObject item = new JSONObject();
                            item.put(JSONKeys.DOCID, docids[i]);
                            item.put(JSONKeys.TITLE,jObj.get(JSONKeys.TITLE));
                            jArray.add(item);
                        }
                    }
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
