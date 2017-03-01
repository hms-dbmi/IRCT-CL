package edu.harvard.hms.dbmi.bd2k.irct.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;

import edu.harvard.hms.dbmi.bd2k.irct.exception.ProcessException;
import edu.harvard.hms.dbmi.bd2k.irct.model.process.IRCTProcess;

public class ProcessControllerTests {
	ProcessController pc;

	@Before
	public void setup() {

		pc = new ProcessController();
		pc.entityManager = mock(EntityManager.class);
		
		IRCTProcess process_test = new IRCTProcess();
		process_test.setId(new Long(1));
		when(pc.entityManager.find(IRCTProcess.class, 1l)).thenReturn(
				process_test);

	}

	/*
	 * @Test public void testToMap() throws JsonGenerationException,
	 * JsonMappingException, IOException{ Map<String, Integer> stuff =
	 * ImmutableMap.of("foo", 2, "bar",4); ObjectMapper mapper = new
	 * ObjectMapper(); System.out.println(mapper.writeValueAsString(stuff)); }
	 * 
	 * @Test public void testFromMap() throws Exception { ObjectMapper mapper =
	 * new ObjectMapper(); Map<String, Integer> output =
	 * mapper.readValue("{\"foo\":2,\"bar\":4}".getBytes(), Map.class);
	 * for(Entry e : output.entrySet()){ System.out.println(e.getKey() + " : " +
	 * e.getValue()); } }
	 */

	@Test
	public void startProcess_test() {
		pc.createProcess();
		pc.getProcess().setId(new Long(1));

		// When creating a process, the initial ID is null
		assertEquals(pc.getProcess().getId(), new Long(1));
	}

	@Test
	public void saveProcess_test() throws ProcessException {
		pc.createProcess();
		pc.getProcess().setId(new Long(1));
		pc.saveProcess();
		assertEquals(pc.getProcess().getId(), new Long(1));
	}

	@Test
	public void getProcess_test() {
		pc.createProcess();
		pc.getProcess().setId(new Long(1));
		assertEquals(pc.getProcess().getId(), new Long(1));
	}

	@Test
	public void loadProcess_test() throws ProcessException {
		pc.loadProcess(new Long(1));
		assertEquals(pc.getProcess().getId(), new Long(1));
	}
}
