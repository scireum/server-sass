package com.glide.scss.compiler;

import java.util.logging.Logger;

import org.testng.annotations.BeforeClass;

public abstract class AbstractTestCase {

	Logger Log = null;

	@BeforeClass
	public final void beginTestClass ()  throws Exception
	{
		Class<? extends AbstractTestCase> currTestClass = getClass ();
		this.Log = Logger.getLogger(currTestClass.getName());

		String message = "Started executing class " + this.getClass().getName();
		Log.info (message);
	}
}
