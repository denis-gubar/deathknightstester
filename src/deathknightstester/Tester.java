/**
 * 
 */
package deathknightstester;
import java.util.HashMap;
import java.util.Date;
import com.topcoder.client.contestant.ProblemComponentModel;
import com.topcoder.shared.language.Language;
import com.topcoder.shared.problem.*;

/**
 * @author DEathkNIghtS
 *
 */
public class Tester
{
	// Map used to store my tags
	private HashMap<String, String> tags = new HashMap<String, String>();
	// Constants
	private static final String WRITERCODE = "$WRITERCODE$";
	private static final String TESTCODE = "$TESTCODE$";
	private static final String RESULTTYPE = "$RESULTTYPE$";
	private static final String NORMALIZEDMETHODPARMS = "$NORMALIZEDMETHODPARMS$";
	private static final String VERSION = "\n// Powered by DEathkNIghtSTester 1.3 31-Dec-2017";
	// Cut tags
	private static final String BEGINCUT = "// BEGIN CUT HERE";
	private static final String ENDCUT = "// END CUT HERE";
	// Problem-related variables
	private ProblemComponentModel problemModel = null;
	private Language language = null;
	/**
	 * PreProcess the source code First determines if it is saved code, writer code, or nothing and
	 * stores it in $WRITERCODE$ tag Secondly builds a main method with default test cases
	 * 
	 * @param source
	 * @param model
	 * @param lang
	 * @param renderer
	 * @return
	 */
	public String preProcess(
		String source,
		ProblemComponentModel model,
		Language lang,
		Renderer renderer)
	{
		// Set defaults for the tags in case we exit out early
		tags.put(WRITERCODE, "");
		tags.put(TESTCODE, "");
		tags.put(RESULTTYPE, "");
		tags.put(NORMALIZEDMETHODPARMS, "");
		// If there is source, return it
		if (source.length() > 0)
			return source;
		// Check to see if the component has any signature
		if (!model.hasSignature())
		{
			tags.put(TESTCODE, "// *** WARNING *** Problem has no signature defined for it");
			return "";
		}
		// Get the test cases
		TestCase[] TestCases = model.getTestCases();
		// Check to see if test cases are defined
		if ((TestCases == null) || (TestCases.length == 0))
		{
			tags.put(TESTCODE, "// *** WARNING *** No test cases defined for this problem");
			return "";
		}
		// Re-initialize the tags
		problemModel = model;
		language = lang;
		tags.clear();
		tags.put(
			RESULTTYPE,
			model.getReturnType().getDescriptor(language));
		DataType[] paramtypes = problemModel.getParamTypes();
		String[] paramnames = problemModel.getParamNames();
		StringBuffer S = new StringBuffer();
		for (int i = 0; i < paramtypes.length; ++i)
		{
			S.append(paramtypes[i].getDescriptor(language));
			S.append(" ");
			S.append(paramnames[i]);
			if (i < paramtypes.length - 1)
				S.append(", ");
		}
		tags.put(NORMALIZEDMETHODPARMS, S.toString());
		tags.put(
			WRITERCODE,
			model.getDefaultSolution().replaceAll(
				RESULTTYPE,
				tags.get(RESULTTYPE).toString()).replaceAll(
				NORMALIZEDMETHODPARMS,
				tags.get(NORMALIZEDMETHODPARMS).toString()));
		// Generate the test cases
		generate_test_code();
		return "";
	}
	// end of preProcess
	/**
	 * This method will cut the test methods above out
	 * 
	 * @param source
	 * @param language
	 * @return
	 */
	public String postProcess(String source, Language language)
	{
		StringBuffer buffer = new StringBuffer("//Written at " + new Date().toString() + "\n");
		buffer.append(source);
		buffer.append(VERSION);
		return buffer.toString();
	}
	// end of postProcess
	/**
	 * This method will return my tags. This method is ALWAYS called after preProcess()
	 * 
	 * @return a map of my tags
	 */
	public HashMap<String, String> getUserDefinedTags()
	{
		return tags;
	}
	// end of getUserDefinedTags
	/**
	 * This method will generate the code for the test cases.
	 */
	private void generate_test_code()
	{
		StringBuffer code = new StringBuffer();
		DataType[] paramtypes = problemModel.getParamTypes();
		DataType returntype = problemModel.getReturnType();
		TestCase[] cases = problemModel.getTestCases();
		String[] paramnames = problemModel.getParamNames();
		
		// Generate the individual test cases
		for (int i = 0; i < cases.length; ++i)
			generate_test_case("Test" + i, code, paramtypes, paramnames, returntype, cases[i]);

		// Generate template for custom test case
		generate_test_case("MyTest", code, paramtypes, paramnames, returntype, cases[0]);
		// Insert the cut tags
		code.insert(0, BEGINCUT + "\n");
		code.append(ENDCUT);
		tags.put(TESTCODE, code.toString());
	}
	/**
	 * This method will generate the code for one test case.
	 * 
	 * @param index
	 * @param code
	 * @param paramtypes
	 * @param returntype
	 * @param testcase
	 */
	private void generate_test_case(
		String TestName,
		StringBuffer code,
		DataType[] paramtypes,
		String[] paramnames,
		DataType returntype,
		TestCase testcase)
	{
		String[] inputs = testcase.getInput();
		String output = testcase.getOutput();
		/*
		 * Generate code for setting up individual test cases and calling the method with these
		 * parameters.
		 */
		code.append("TEST( TopCoderMain, ");
		code.append(TestName);
		code.append(" )\n"); 
		code.append("{\n");
		code.append("    ");
		code.append(problemModel.getClassName());
		code.append(" ___test;\n");
		// Generate each input variable separately
		for (int i = 0; i < inputs.length; ++i)
			generate_parameter(code, paramtypes[i], "_" + paramnames[i], inputs[i]);
		// Generate the output variable as the last variable
		generate_parameter(code, returntype, "expected_result", output);
		code.append("    TEST_TIMEOUT_BEGIN\n");
		code.append("        EXPECT_EQ( expected_result, ___test.");
		code.append(problemModel.getMethodName());
		code.append("( ");
		for (int i = 0; i < inputs.length; ++i)
		{
			code.append("_" + paramnames[i]);
			if ( i + 1 < inputs.length )
				code.append(",");
			code.append(" ");
		}
		code.append(") );\n");
		code.append("    TEST_TIMEOUT_FAIL_END( 2000 )\n");
		code.append("}\n");
		code.append("\n");		
	}
	/**
	 * This method will generate the required parameter as a unique variable.
	 * 
	 * @param index
	 * @param code
	 * @param paramtype
	 * @param input
	 */
	private void generate_parameter(StringBuffer code, DataType paramtype, String paramname, String input)
	{
		String typename = paramtype.getDescriptor(language);
		code.append("    ");
		code.append(typename);
		code.append(" "); 
		code.append(paramname);
		code.append(" = ");
		code.append(input);
		code.append(";\n");
	}
}