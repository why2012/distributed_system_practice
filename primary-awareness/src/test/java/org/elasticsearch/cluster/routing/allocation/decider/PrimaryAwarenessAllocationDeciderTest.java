package org.elasticsearch.cluster.routing.allocation.decider;

import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.primaryaware.PrimaryAwarenessPlugin;
import org.elasticsearch.test.ESIntegTestCase;

import java.util.Collection;
import java.util.Collections;

import static org.elasticsearch.test.ESIntegTestCase.Scope.SUITE;

@ESIntegTestCase.ClusterScope(scope = SUITE, supportsDedicatedMasters = false, numDataNodes = 1, numClientNodes = 0)
public class PrimaryAwarenessAllocationDeciderTest extends ESIntegTestCase {

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Collections.singletonList(PrimaryAwarenessPlugin.class);
    }

    @Override
    protected Collection<Class<? extends Plugin>> transportClientPlugins() {
        return Collections.singletonList(PrimaryAwarenessPlugin.class);
    }

    public void testPrimaryAwarenessAllocationDecider() {}
}
