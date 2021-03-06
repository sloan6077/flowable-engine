/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.engine.test.bpmn.servicetask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.impl.jobexecutor.AsyncContinuationJobHandler;
import org.flowable.engine.impl.jobexecutor.AsyncTriggerJobHandler;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.Test;

public class TriggerableServiceTaskTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testClassDelegate() {
        String processId = runtimeService.startProcessInstanceByKey("process").getProcessInstanceId();

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("service1").singleResult();
        assertNotNull(execution);
        int count = (int) runtimeService.getVariable(processId, "count");
        assertEquals(1, count);

        Map<String,Object> processVariables = new HashMap<>();
        processVariables.put("count", ++count);
        runtimeService.trigger(execution.getId(), processVariables, null);

        execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("usertask1").singleResult();
        assertNotNull(execution);
        assertEquals(3, runtimeService.getVariable(processId, "count"));
    }

    @Test
    @Deployment
    public void testDelegateExpression() {
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("triggerableServiceTask", new TriggerableServiceTask());

        String processId = runtimeService.startProcessInstanceByKey("process", varMap).getProcessInstanceId();

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("service1").singleResult();
        assertNotNull(execution);
        int count = (int) runtimeService.getVariable(processId, "count");
        assertEquals(1, count);

        Map<String,Object> processVariables = new HashMap<>();
        processVariables.put("count", ++count);
        runtimeService.trigger(execution.getId(), processVariables, null);

        execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("usertask1").singleResult();
        assertNotNull(execution);
        assertEquals(3, runtimeService.getVariable(processId, "count"));
    }

    @Test
    @Deployment
    public void testAsyncJobs() {
        String processId = runtimeService.startProcessInstanceByKey("process").getProcessInstanceId();
        
        List<Job> jobs = managementService.createJobQuery().processInstanceId(processId).list();
        assertEquals(1, jobs.size());
        assertEquals(AsyncContinuationJobHandler.TYPE, jobs.get(0).getJobHandlerType());
        
        waitForJobExecutorToProcessAllJobs(7000L, 250L);

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("service1").singleResult();
        assertNotNull(execution);
        int count = (int) runtimeService.getVariable(processId, "count");
        assertEquals(1, count);

        Map<String,Object> processVariables = new HashMap<>();
        processVariables.put("count", ++count);
        runtimeService.triggerAsync(execution.getId(), processVariables);
        
        jobs = managementService.createJobQuery().processInstanceId(processId).list();
        assertEquals(1, jobs.size());
        assertEquals(AsyncTriggerJobHandler.TYPE, jobs.get(0).getJobHandlerType());
        
        waitForJobExecutorToProcessAllJobs(7000L, 250L);

        execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("usertask1").singleResult();
        assertNotNull(execution);
        assertEquals(3, runtimeService.getVariable(processId, "count"));
    }
}
