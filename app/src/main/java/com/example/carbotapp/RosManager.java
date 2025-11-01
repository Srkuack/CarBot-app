package com.example.carbotapp;

import android.content.Context;

import org.ros.address.InetAddressFactory;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.NodeMainExecutor;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.topic.Publisher;

import java.net.URI;

public class RosManager {

    private static NodeMainExecutor executor;
    private static IO ioNode;   // nodo interno que publica tópicos

    // ---------- Ciclo de conexión ----------
    public static void connect(Context ctx, String masterUriStr) {
        if (executor == null) executor = DefaultNodeMainExecutor.newDefault();
        if (ioNode == null)   ioNode   = new IO();

        URI masterUri = URI.create(masterUriStr);
        String host   = InetAddressFactory.newNonLoopback().getHostAddress();

        NodeConfiguration cfg = NodeConfiguration.newPublic(host);
        cfg.setMasterUri(masterUri);

        executor.execute(ioNode, cfg);
    }

    public static void disconnect(Context ctx) {
        if (executor != null && ioNode != null) {
            executor.shutdownNodeMain(ioNode);
        }
        ioNode   = null;
        executor = null;
    }

    // ---------- API que llama la Activity ----------
    /** Publica velocidad lineal (rpm ~ -1000..1000) y opcionalmente dirección a partir de angularZ. */
    public static void setVelocity(double linearX, double angularZ) {
        if (ioNode != null) ioNode.setVelocity(linearX, angularZ);
    }

    /** Publica dirección ya calculada (0..180; 90 centro). */
    public static void publishSteering(short degrees) {
        if (ioNode != null) ioNode.publishSteering(degrees);
    }

    /** Publica modo de direccionales (e.g., "left_on", "right_on", "left_off"). */
    public static void publishBlinker(String mode) {
        if (ioNode != null) ioNode.publishBlinker(mode);
    }

    // ==========================================================
    //                Nodo interno que publica a ROS
    // ==========================================================
    private static class IO implements NodeMain {

        private ConnectedNode node;

        private Publisher<std_msgs.Int16>  speedPub;     // /manual_control/speed
        private Publisher<std_msgs.Int16>  steeringPub;  // /manual_control/steering
        private Publisher<std_msgs.String> blinkerPub;   // /led

        private static final String TOPIC_SPEED    = "/manual_control/speed";
        private static final String TOPIC_STEERING = "/manual_control/steering";
        private static final String TOPIC_BLINKER  = "/led";

        @Override public GraphName getDefaultNodeName() {
            return GraphName.of("android/ros_manager_io");
        }

        @Override
        public void onStart(ConnectedNode connectedNode) {
            node = connectedNode;

            speedPub    = node.newPublisher(TOPIC_SPEED,    std_msgs.Int16._TYPE);
            steeringPub = node.newPublisher(TOPIC_STEERING, std_msgs.Int16._TYPE);
            blinkerPub  = node.newPublisher(TOPIC_BLINKER,  std_msgs.String._TYPE);
        }

        @Override public void onShutdown(Node n) {}
        @Override public void onShutdownComplete(Node n) {}
        @Override public void onError(Node n, Throwable t) { t.printStackTrace(); }

        // ------- Implementación de la lógica de publicación -------

        /** Convierte linearX a -1000..1000 y publica; angularZ -> 0..180 (90 centro). */
        public void setVelocity(double linearX, double angularZ) {
            // Velocidad
            if (speedPub != null) {
                std_msgs.Int16 m = speedPub.newMessage();
                int rpm = (int) Math.round(linearX * 1000.0);
                if (rpm < -1000) rpm = -1000;
                if (rpm >  1000) rpm =  1000;
                m.setData((short) rpm);
                speedPub.publish(m);
            }

            // Si quieres que también controle dirección desde angularZ, descomenta:
            // int steer = (int) Math.round(90.0 + angularZ * 45.0);
            // publishSteering((short) steer);
        }

        /** Publica el ángulo de dirección (0..180, 90 centro). */
        public void publishSteering(short degrees) {
            if (steeringPub == null) return;
            int steer = degrees;
            if (steer < 0)   steer = 0;
            if (steer > 180) steer = 180;

            std_msgs.Int16 m = steeringPub.newMessage();
            m.setData((short) steer);
            steeringPub.publish(m);
        }

        /** Publica el estado de blinkers. */
        public void publishBlinker(String mode) {
            if (blinkerPub == null) return;
            std_msgs.String b = blinkerPub.newMessage();
            b.setData(mode);
            blinkerPub.publish(b);
        }
    }
}
