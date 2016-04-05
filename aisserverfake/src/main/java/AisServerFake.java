import dk.dma.ais.binary.SixbitException;
import dk.dma.ais.message.AisMessage1;
import dk.dma.ais.message.AisPosition;
import dk.dma.ais.sentence.Vdm;
import dk.dma.enav.model.geometry.Position;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by cameron on 29/03/16.
 */
public class AisServerFake implements Runnable {
    private Socket sock;
    private PrintWriter out = null;

    public AisServerFake(Socket sock) throws IOException {
        this.sock = sock;
        try {
            out = new PrintWriter(sock.getOutputStream(), true);
        } catch (IOException e) {
            sock.close();
        }
    }

    public static void main(String args[]) throws IOException {
        ServerSocket ss = new ServerSocket(11010);
        while (!ss.isClosed()) {
            Socket sock = ss.accept();
            Thread trd = new Thread(new AisServerFake(sock));
            trd.start();
        }
    }

    @Override
    public void run() {
        while (sock != null && !sock.isClosed()) {
            AisMessage1 msg1 = new AisMessage1();
            msg1.setRepeat(0);
            msg1.setUserId(265410000);
            msg1.setNavStatus(0);
            msg1.setRot(0);
            msg1.setSog(201);
            msg1.setPosAcc(1);
            AisPosition pos = new AisPosition(Position.create(randomLat(), randomLon()));
            msg1.setPos(pos);
            msg1.setCog(732);
            msg1.setTrueHeading(76);
            msg1.setUtcSec(42);
            msg1.setSpecialManIndicator(0);
            msg1.setSpare(0);
            msg1.setRaim(0);
            msg1.setSyncState(0);
            msg1.setSlotTimeout(0);
            msg1.setSubMessage(2230);

            Vdm vdm = new Vdm();
            vdm.setTalker("AI");
            vdm.setTotal(1);
            vdm.setNum(1);
            try {
                vdm.setMessageData(msg1);
            } catch (SixbitException e) {
                sock = null;
            }
            vdm.setChannel('A');

            String encoded = vdm.getEncoded();

            out.println(encoded);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                sock = null;
            }
        }

    }

    private double randomLat() {
        //return -37.663712; //melbourne
        return Math.random()*180-90;
    }

    private double randomLon() {
        //return 144.844788; //melbourne
        return Math.random()*360-180;
    }
}
