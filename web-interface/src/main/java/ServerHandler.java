import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Created by cameron on 21/03/16.
 */
public class ServerHandler extends AbstractHandler
{
    DataHandler data;
    ServerHandler() {
        try {
            data = new DataHandler();
        } catch (AccumuloSecurityException e) {
            e.printStackTrace();
        } catch (AccumuloException e) {
            e.printStackTrace();
        }
    }
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
            throws IOException, ServletException
    {
        String file = request.getPathInfo();
        if (file.contains("/geosearch")) {
            data.handle(target, baseRequest, request, response);
            return;
        }
        response.setContentType("text/html;charset=utf-8");
        if (file.compareTo("/") == 0) {
            file = "index.html";
        }
        URL url = ServerHandler.class.getResource(file);
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        if (url == null) {
            url = ServerHandler.class.getResource("404.html");
        }
        InputStream is = url.openConnection().getInputStream();
        BufferedReader reader = new BufferedReader( new InputStreamReader( is )  );
        String line;
        while((line = reader.readLine()) != null) {
            response.getWriter().println(line);
        }
    }

    public static void main(String[] args) throws Exception
    {
        Server server = new Server(8123);
        server.setHandler(new ServerHandler());

        server.start();
        server.join();
    }
}