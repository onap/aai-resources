package org.onap.aai.migration.v12;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.onap.aai.db.props.AAIProperties;
import org.onap.aai.migration.*;
import org.onap.aai.serialization.engines.TransactionalGraphEngine;
import org.onap.aai.util.AAIConstants;

import java.io.*;
import java.util.Optional;

@MigrationPriority(20)
@MigrationDangerRating(2)
@Enabled
public class MigrateDataFromASDCToConfiguration extends Migrator {
    private final String PARENT_NODE_TYPE = "generic-vnf";
    private boolean success = true;
    private String entitlementPoolUuid = "";
    private String VNT = "";


    public MigrateDataFromASDCToConfiguration(TransactionalGraphEngine engine) {
        super(engine);
    }


    @Override
    public void run() {
        String csvFile = AAIConstants.AAI_HOME_ETC + "VNT-migration-data" + AAIConstants.AAI_FILESEP + "VNT-migration-input.csv";
        logger.info("Reading Csv file: " + csvFile);
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = "\t";
        try {

            br = new BufferedReader(new FileReader(new File(csvFile)));
            while ((line = br.readLine()) != null) {
                line = line.replaceAll("\"", "");
                String[] temp = line.split(cvsSplitBy);
                if ("entitlement-pool-uuid".equals(temp[0]) || "vendor-allowed-max-bandwidth (VNT)".equals(temp[1])) {
                    continue;
                }
                entitlementPoolUuid = temp[0];
                VNT = temp[1];
                GraphTraversal<Vertex, Vertex> f = this.engine.asAdmin().getTraversalSource().V().has(AAIProperties.NODE_TYPE, "entitlement").has("group-uuid", entitlementPoolUuid)
                        .out("org.onap.relationships.inventory.BelongsTo").has(AAIProperties.NODE_TYPE, "generic-vnf")
                        .has("vnf-type", "vHNF").in("org.onap.relationships.inventory.ComposedOf").has(AAIProperties.NODE_TYPE, "service-instance").out("org.onap.relationships.inventory.Uses").has(AAIProperties.NODE_TYPE, "configuration");
                
                modify(f);
            }

        } catch (FileNotFoundException e) {
            success = false;
            logger.error("Found Exception" , e);
        } catch (IOException e) {
            success = false;
            logger.error("Found Exception" , e);
        } catch (Exception a) {
            success= false;
            logger.error("Found Exception" , a);
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                success = false;
                logger.error("Found Exception" , e);
            }
        }

    }

    public void modify(GraphTraversal<Vertex, Vertex> g) {
        int count = 0;
        while (g.hasNext()) {
            Vertex v = g.next();
            logger.info("Found node type " + v.property("aai-node-type").value().toString() + " with configuration id:  " + v.property("configuration-id").value().toString());
            v.property("vendor-allowed-max-bandwidth", VNT);
            logger.info("VNT val after migration: " + v.property("vendor-allowed-max-bandwidth").value().toString());
            count++;
        }

        logger.info("modified " + count + " configuration nodes related to Entitlement UUID: " +entitlementPoolUuid);

    }

    @Override
    public Status getStatus() {
        if (success) {
            return Status.SUCCESS;
        } else {
            return Status.FAILURE;
        }
    }

    @Override
    public Optional<String[]> getAffectedNodeTypes() {
        return Optional.of(new String[]{PARENT_NODE_TYPE});
    }

    @Override
    public String getMigrationName() {
        return "MigrateDataFromASDCToConfiguration";
    }


}
