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
import calliope.core.database.Connection;
import calliope.core.exception.DbException;
import calliope.core.database.Connector;
import calliope.core.constants.Formats;
import calliope.core.constants.JSONKeys;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import misc.exception.MiscException;
import misc.constants.Params;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Get the previous document in the MISC collection
 * @author desmond
 */
public class MiscPreviousHandler extends MiscGetHandler {
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws MiscException
    {
        try
        {
            JSONObject jObj = new JSONObject();
            String docid = request.getParameter(Params.DOCID);
            if ( docid != null )
            {
                Connection conn = Connector.getConnection();
                String[] parts = docid.split("/");
                for ( int i=parts.length-1;i>0;i-- )
                {
                    StringBuilder sb = new StringBuilder();
                    for ( int j=0;j<i;j++ )
                    {
                        if ( sb.length()>0 )
                            sb.append("/");
                        sb.append(parts[j]);
                    }
                    String testID = sb.toString();
                    String[] found = conn.listDocuments(Database.MISC,
                        testID+"/.*", JSONKeys.DOCID);
                    for ( int k=found.length-1;k>=0;k-- )
                    {
                        String lastID = found[k];
                        String jStr = conn.getFromDb(Database.MISC,lastID);
                        if ( jStr != null )
                        {
                            JSONObject bson = (JSONObject)JSONValue.parse(jStr);
                            String format = (String)bson.get(JSONKeys.FORMAT);
                            if ( format != null && format.equals(Formats.MIME_MARKDOWN) )
                            {
                                if ( !lastID.equals(docid) )
                                {
                                    String title = makeTitle( lastID );
                                    jObj.put(JSONKeys.TITLE,title);
                                    jObj.put(JSONKeys.DOCID, lastID);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            else
                throw new Exception("Missing docid");
            if ( !jObj.containsKey(JSONKeys.TITLE) )
                throw new DbException("No suitable previous document");
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            response.getOutputStream().println(strip(jObj.toJSONString()));
        }
        catch ( Exception e )
        {
            throw new MiscException(e);
        }
    }
}
