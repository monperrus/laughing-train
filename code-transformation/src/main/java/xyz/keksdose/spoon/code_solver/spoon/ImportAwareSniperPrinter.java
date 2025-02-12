
package xyz.keksdose.spoon.code_solver.spoon;

import spoon.compiler.Environment;
import spoon.reflect.code.CtLambda;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtImport;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.support.sniper.SniperJavaPrettyPrinter;
import spoon.support.util.ModelList;

public class ImportAwareSniperPrinter extends SniperJavaPrettyPrinter {

	public ImportAwareSniperPrinter(Environment env) {
		super(env);
	}

	@Override
	protected ModelList<CtImport> getImports(CtCompilationUnit compilationUnit) {
		return compilationUnit.getImports();
	}

	@Override
	public void visitCtImport(CtImport ctImport) {
		if (ctImport instanceof NewlineImport) {
			String s = getPrinterTokenWriter().getPrinterHelper().toString();
			if (!endsWithNewline(s)) {
				getPrinterTokenWriter().writeln();
			}
			return;
		}
		super.visitCtImport(ctImport);
	}

	private boolean endsWithNewline(String s) {
		return s.endsWith("\n") || s.endsWith("\r\n");
	}

	@Override
	public <T> void visitCtLambda(CtLambda<T> lambda) {
		enterCtExpression(lambda);
		// single parameter lambdas dont need to be wrapped in parentheses
		if (isSingleParameterWithoutExplicitType(lambda)) {
			getElementPrinterHelper().printList(lambda.getParameters(), null, false, "", false, false, ",", false,
				false, "", this::scan);
		}
		else {
			getElementPrinterHelper().printList(lambda.getParameters(), null, false, "(", false, false, ",", false,
				false, ")", this::scan);
		}
		getPrinterTokenWriter().writeSpace();
		getPrinterTokenWriter().writeSeparator("->");
		getPrinterTokenWriter().writeSpace();
		if (lambda.getBody() != null) {
			scan(lambda.getBody());
		}
		else {
			scan(lambda.getExpression());
		}
		exitCtExpression(lambda);
	}

	private <T> boolean isSingleParameterWithoutExplicitType(CtLambda<T> lambda) {
		return lambda.getParameters().size() == 1 && (lambda.getParameters().get(0).getType() == null
				|| lambda.getParameters().get(0).getType().isImplicit());
	}

	@Override
	public <T> void visitCtParameter(CtParameter<T> parameter) {
		getElementPrinterHelper().writeComment(parameter);
		getElementPrinterHelper().writeAnnotations(parameter);
		getElementPrinterHelper().writeModifiers(parameter);
		if (parameter.isVarArgs()) {
			scan(((CtArrayTypeReference<T>) parameter.getType()).getComponentType());
			getPrinterTokenWriter().writeSeparator("...");
		}
		else if (parameter.isInferred() && this.env.getComplianceLevel() >= 11) {
			getPrinterTokenWriter().writeKeyword("var");
		}
		else {
			scan(parameter.getType());
		}
		// dont write spaces if the type is implicit
		if (parameter.getType() != null && !parameter.getType().isImplicit()) {
			getPrinterTokenWriter().writeSpace();
		}
		getPrinterTokenWriter().writeIdentifier(parameter.getSimpleName());
	}
}
