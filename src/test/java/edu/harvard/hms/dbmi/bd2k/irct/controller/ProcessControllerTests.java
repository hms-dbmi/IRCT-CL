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
	IRCTProcess process_test = new IRCTProcess();

	@Before
	public void setup() {

		pc = new ProcessController();
		pc.entityManager = mock(EntityManager.class);

		process_test.setId(new Long(1));
		when(pc.entityManager.find(IRCTProcess.class, 1l)).thenReturn(
				process_test);

	}

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
		pc.createProcess();
		pc.loadProcess(new Long(1));
		assertEquals(pc.getProcess().getId(), new Long(1));
	}
}
