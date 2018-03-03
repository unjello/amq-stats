package pl.lichnerowicz.hcsvntdracones;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ActiveMQStats {
    private static final Logger LOG = LogManager.getLogger(ActiveMQStats.class);

    private static Set<Attribute> getAttributes(MBeanServerConnection connection, ObjectName name, String... strings)
            throws InstanceNotFoundException, IOException, ReflectionException {
        AttributeList list = connection.getAttributes(name, strings);
        if (list != null) //noinspection unchecked
            return new HashSet<Attribute>((List) list);
        return new HashSet<>();

    }

    public static void main(String[] args)  {
        LOG.info("ActiveMQStats starting");
        LOG.debug("Command line (" + args.length +"): " + String.join(", ", args));
        if (args.length < 1) {
            LOG.fatal("JMX port not provided.");
            System.err.println("Usage: amq-stats <jmxport>");
            System.exit(1);
        }
        try {
            JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://:" + args[0] + "/jmxrmi");
            LOG.info("Connected to JMX at: " + url.toString());

            try (JMXConnector jmxc = JMXConnectorFactory.connect(url, null)) {
                MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
                String brokerObjectName = "org.apache.activemq:type=Broker,brokerName=localhost";
                Set<ObjectInstance> mbeans = mbsc.queryMBeans(
                        new ObjectName(brokerObjectName),
                        null
                );
                LOG.debug(brokerObjectName + " returned " + mbeans.size() + " objects");

                String queuesObjectNamePattern = "org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Queue,destinationName=*";
                Set<ObjectInstance> mbeans_queues = mbsc.queryMBeans(
                        new ObjectName(queuesObjectNamePattern),
                        null
                );
                LOG.debug(queuesObjectNamePattern + " returned " + mbeans_queues.size() + " objects");

                ObjectInstance broker = mbeans.toArray(new ObjectInstance[0])[0];
                ObjectInstance[] queues = mbeans_queues.toArray(new ObjectInstance[0]);

                Set<Attribute> ba = getAttributes(mbsc, broker.getObjectName(), "TotalMessageCount", "TotalEnqueueCount", "TotalDequeueCount", "TotalProducerCount", "TotalConsumerCount", "TotalConnectionsCount", "CurrentConnectionsCount");
                Set<Attribute> qa = getAttributes(mbsc, queues[0].getObjectName(), "Name", "QueueSize", "ProducerCount", "ConsumerCount", "EnqueueCount", "InFlightCount", "DequeueCount", "MinEnqueueTime", "AverageEnqueueTime", "MaxEnqueueTime", "TotalBlockedTime");
                qa.addAll(ba);
                String headers = String.join(",", qa.stream().map(Attribute::getName).collect(Collectors.toList()));
                System.out.println(headers);
                LOG.debug("Metric headers: " + headers);

                do {
                    Set<Attribute> brokerAttr = getAttributes(mbsc, broker.getObjectName(), "TotalMessageCount", "TotalEnqueueCount", "TotalDequeueCount", "TotalProducerCount", "TotalConsumerCount", "TotalConnectionsCount", "CurrentConnectionsCount");
                    for (ObjectInstance queue : queues) {
                        Set<Attribute> queueAttr = getAttributes(mbsc, queue.getObjectName(), "Name", "QueueSize", "ProducerCount", "ConsumerCount", "EnqueueCount", "InFlightCount", "DequeueCount", "MinEnqueueTime", "AverageEnqueueTime", "MaxEnqueueTime", "TotalBlockedTime");
                        queueAttr.addAll(brokerAttr);
                        String metrics = String.join(",", queueAttr.stream().map((Attribute a) -> a.getValue().toString()).collect(Collectors.toList()));
                        System.out.println(metrics);
                        LOG.debug("Metric report: " + metrics);
                        Thread.sleep(100);
                    }
                } while (true);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
