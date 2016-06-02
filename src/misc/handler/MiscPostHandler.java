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

import calliope.core.constants.JSONKeys;
import calliope.core.database.*;
import calliope.core.Base64;
import calliope.core.handler.Handler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import calliope.core.exception.DbException;
import calliope.core.constants.Database;
import calliope.core.constants.Formats;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import misc.constants.Params;
import misc.exception.MiscException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.JSONArray;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * Handle posting or saving of project data
 * @author desmond
 */
public class MiscPostHandler extends Handler
{
    String docid;
    String text;
    String encoding;
    String format;
    String fileName;
    String title;
    String delete;
    JSONObject userdata;
    
    public MiscPostHandler()
    {
        this.encoding = "UTF-8";
        this.format = Formats.MIME_MARKDOWN;
    }
    /**
     * Process a field we recognise
     * @param fieldName the field's name
     * @param contents its contents
     */
    void processField( String fieldName, String contents )
    {
        //System.out.println("Received field "+fieldName);
        if ( fieldName.equals(Params.DOCID) )
        {
            int index = contents.lastIndexOf(".");
            if ( index != -1 )
                contents = contents.substring(0,index);
            docid = contents;
        }
        else if ( fieldName.equals(Params.ENCODING) )
        {
            encoding = contents;
        }
        else if ( fieldName.equals(Params.TEXT))
            text = contents;
        else if ( fieldName.equals(Params.FORMAT) )
            format = contents;
        else if ( fieldName.equals(Params.TITLE) )
            title = contents;
        else if ( fieldName.equals(Params.DELETE) )
            delete = contents;
        else if ( fieldName.equals(Params.USERDATA) )
        {
            String key = "I tell a settlers tale of the old times";
            int klen = key.length();
            char[] data = Base64.decode( contents );
            StringBuilder sb = new StringBuilder();
            for ( int i=0;i<data.length;i++ )
                sb.append((char)(data[i]^key.charAt(i%klen)));
            String json = sb.toString();
//            System.out.println( "USERDATA: decoded json data="+json);
            userdata = (JSONObject)JSONValue.parse(json);
//            System.out.println("json="+json);
//            System.out.println( "user was "+userdata.get(JSONKeys.NAME));
            JSONArray roles = (JSONArray)userdata.get(JSONKeys.ROLES);
//            if ( roles.size()>0 )
//                System.out.println("role was "+roles.get(0));
        }
    }
    /**
     * Parse the import params from the request
     * @param request the http request
     */
    void parseImportParams( HttpServletRequest request ) throws MiscException
    {
        try
        {
            boolean isMultipart = ServletFileUpload.isMultipartContent(request);
            //System.out.println("Parsing import params");
            if ( isMultipart )
            {
                FileItemFactory factory = new DiskFileItemFactory();
                // Create a new file upload handler
                ServletFileUpload upload = new ServletFileUpload(factory);
                // Parse the request
                List items = upload.parseRequest( request );
                for ( int i=0;i<items.size();i++ )
                {
                    FileItem item = (FileItem) items.get( i );
                    if ( item.isFormField() )
                    {
                        String fieldName = item.getFieldName();
                        if ( fieldName != null )
                        {
                            String contents = item.getString("UTF-8");
                            processField(fieldName,contents);
                        }
                    }
                    else if ( item.getName().length()>0 )
                    {
                        fileName = item.getName();
                        // item.getName retrieves the ORIGINAL file name
                        format = item.getContentType();
                        if ( format != null )
                        {
                            if ( format.startsWith("text/") )
                            {
                                InputStream is = item.getInputStream();
                                ByteArrayOutputStream bh = new ByteArrayOutputStream();
                                while ( is.available()>0 )
                                {
                                    byte[] b = new byte[is.available()];
                                    is.read( b );
                                    bh.write( b );
                                }
                                text = new String(bh.toByteArray(), encoding );
                            }
                            else
                                System.out.println("skipping file type"+format);
                        }
                    }
                }
            }
            else
            {
                Map tbl = request.getParameterMap();
                Set<String> keys = tbl.keySet();
                Iterator<String> iter = keys.iterator();
                while ( iter.hasNext() )
                {
                    String key = iter.next();
                    String[] values = (String[])tbl.get(key);
                    for ( int i=0;i<values.length;i++ )
                        processField( key, values[i]);
                }
            }
        }
        catch ( Exception e )
        {
            throw new MiscException( e );
        }
    }
    /**
     * Check if the current user is able to update or delete the resource
     * @param conn the current database connection
     * @param request the servlet response
     * @return true if he/she is else false
     */
    boolean checkUser( Connection conn, HttpServletResponse response ) 
        throws DbException
    {
        String jStr = conn.getFromDb(Database.MISC,docid);
        if ( jStr != null )
        {
            JSONObject miscDoc = (JSONObject) JSONValue.parse( jStr );
            if ( miscDoc.containsKey(JSONKeys.OWNER) )
            {
                String owner = (String) miscDoc.get(JSONKeys.OWNER);
                JSONArray roles = (JSONArray)userdata.get(JSONKeys.ROLES);
                if ( userdata != null && userdata.containsKey(JSONKeys.NAME) 
                    && ((String)userdata.get(JSONKeys.NAME)).equals(owner) )
                        return true;
                else if ( roles.size()>0 && roles.contains("editor") )
                    return true;
                else
                {
                    try
                    {
                        response.getWriter().println( "Status: 403; User "
                            +userdata.get(JSONKeys.USER)
                            +" is not authorised to alter resource "+docid);
                    }
                    catch ( Exception e )
                    {
                        System.out.println( e.toString() );
                    }
                    return false;
                }
            }
            else    // no protection = anyone can delete/update
                return true;
        }
        else // new document
        {
            JSONArray roles = (JSONArray)userdata.get(JSONKeys.ROLES);
            return roles.size()>0 && roles.contains("editor");
        }
    }
     /**
     * Handle a POST request
     * @param request the raw request
     * @param response the response we will write to
     * @param urn the rest of the URL after stripping off the context
     * @throws ProjectException 
     */
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws MiscException
    {
        try
        {
            //System.out.println("About to parse params");
            parseImportParams( request );
            //System.out.println("Parsed params");
            JSONObject jDoc = new JSONObject();
            jDoc.put(JSONKeys.FORMAT, format );
            if ( title != null )
                jDoc.put(JSONKeys.TITLE, title );
            Connection conn = Connector.getConnection();
            if ( delete != null && delete.equals("true") 
                && docid!= null && docid.length()>0 && checkUser(conn,response) )
            {
                conn.removeFromDb(Database.MISC, docid);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().println( "Status: 200; Removed "+docid );
            }
            else if ( text != null && checkUser(conn,response) ) // update or new
            {
                //System.out.println("About to create or update");
                jDoc.put(JSONKeys.BODY,text);
                // if updating owner will already be userdata.user
                jDoc.put(JSONKeys.OWNER,userdata.get(JSONKeys.USER));
                conn.putToDb( Database.MISC, docid, jDoc.toJSONString() );
                response.setContentType("text/plain;charset=UTF-8");
                if ( fileName == null )
                    fileName = "direct submit";
                response.getWriter().println( "Status: 200; Stored "+text.length()
                    +" chars from "+fileName+" at "+docid );
            }
            else // if we're using CURL
            {
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().println( "Status: 400; Invalid content-type ("
                        +format+") for fileName "+fileName );
            }
        }
        catch ( Exception e )
        {
            try {
                response.getWriter().println( "Status: 500; Exception "+e.getMessage());
            } 
            catch (Exception ex )
            {}
            System.out.println(e.getMessage() );
            throw new MiscException(e);
        }
    }
}
