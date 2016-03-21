package lombok.javac.handlers;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;
import lombok.AccessLevel;
import lombok.FieldConstants;
import lombok.core.AST.Kind;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import org.mangosdk.spi.ProviderFor;

import java.lang.reflect.Modifier;
import java.util.Collection;

import static lombok.javac.handlers.JavacHandlerUtil.*;

@ProviderFor(JavacAnnotationHandler.class)
@SuppressWarnings("restriction")
public class HandleFieldConstants extends JavacAnnotationHandler<FieldConstants> {
	
	public void generateFieldDefaultsForType(JavacNode typeNode, JavacNode errorNode, AccessLevel level, boolean checkForTypeLevelFieldConstants) {
		
		if (checkForTypeLevelFieldConstants) {
			if (hasAnnotation(FieldConstants.class, typeNode)) {
				return;
			}
		}
		
		JCClassDecl typeDecl = null;
		if (typeNode.get() instanceof JCClassDecl) typeDecl = (JCClassDecl) typeNode.get();
		
		long modifiers = typeDecl == null ? 0 : typeDecl.mods.flags;
		boolean notAClass = (modifiers & (Flags.INTERFACE | Flags.ANNOTATION)) != 0;
		
		if (typeDecl == null || notAClass) {
			errorNode.addError("@FieldConstants is only supported on a class or an enum or a field.");
			return;
		}
		
		for (JavacNode field : typeNode.down()) {
			if (fieldQualifiesForFieldConstantsGeneration(field)) generateFieldConstantsForField(field, errorNode.get(), level);
			
		}
	}
	
	private void generateFieldConstantsForField(JavacNode fieldNode, DiagnosticPosition pos, AccessLevel level) {
		if (hasAnnotation(FieldConstants.class, fieldNode)) {
			return;
		}
		createFieldConstantsForField(level, fieldNode, fieldNode, false, List.<JCAnnotation>nil());
	}
	
	private boolean fieldQualifiesForFieldConstantsGeneration(JavacNode field) {
		if (field.getKind() != Kind.FIELD) return false;
		JCVariableDecl fieldDecl = (JCVariableDecl) field.get();
		if (fieldDecl.name.toString().startsWith("$")) return false;
		if ((fieldDecl.mods.flags & Flags.STATIC) != 0) return false;
		return true;
	}

	@Override
	public void handle(AnnotationValues<FieldConstants> annotation, JCAnnotation ast, JavacNode annotationNode) {
    	Collection<JavacNode> fields = annotationNode.upFromAnnotationToFields();
		deleteAnnotationIfNeccessary(annotationNode, FieldConstants.class);
		deleteImportFromCompilationUnit(annotationNode, "lombok.AccessLevel");
		JavacNode node = annotationNode.up();
		FieldConstants annotatationInstance = annotation.getInstance();
		AccessLevel level = annotatationInstance.level();
		if (level == AccessLevel.NONE) {
			annotationNode.addWarning("'lazy' does not work with AccessLevel.NONE.");
			return;
		}
		if (node == null) return;
        System.out.println ("FieldConstants on  "+node.getName());

        List<JCAnnotation> onMethod = unboxAndRemoveAnnotationParameter(ast, "onMethod", "@FieldConstants(onMethod=", annotationNode);
		switch (node.getKind()) {
		case FIELD:
			createFieldConstantsForFields(level, fields, annotationNode, annotationNode, true, onMethod);
			break;
		case TYPE:
			if (!onMethod.isEmpty()) {
				annotationNode.addError("'onMethod' is not supported for @FieldConstants on a type.");
			}
			generateFieldDefaultsForType(node, annotationNode, level, false);
			break;
		}
	}
	
	private void createFieldConstantsForFields(AccessLevel level, Collection<JavacNode> fieldNodes, JavacNode annotationNode, JavacNode errorNode, boolean whineIfExists, List<JCAnnotation> onMethod) {
		for (JavacNode fieldNode : fieldNodes) {
			createFieldConstantsForField(level, fieldNode, errorNode, whineIfExists, onMethod);
		}
	}
	
	private void createFieldConstantsForField(AccessLevel level, JavacNode fieldNode, JavacNode source, boolean whineIfExists, List<JCAnnotation> onMethod) {
		if (fieldNode.getKind() != Kind.FIELD) {
			source.addError("@FieldConstants is only supported on a class or a field");
			return;
		}
		JCVariableDecl field = (JCVariableDecl) fieldNode.get();
		String constantName = camelCaseToConstant(field.name.toString());
		if (constantName == null) {
			source.addWarning("Not generating constant for this field: It does not fit in your @Accessors prefix list");
			return;
		}
		
		JavacTreeMaker treeMaker = fieldNode.getTreeMaker();
		JCModifiers modifiers = treeMaker.Modifiers(toJavacModifier(level) | Modifier.STATIC | Modifier.FINAL);
		JCExpression returnType = chainDots(fieldNode, "java", "lang", "String");
		JCExpression init = treeMaker.Literal(fieldNode.getName());
		JCVariableDecl fieldConstant = treeMaker.VarDef(modifiers, fieldNode.toName(constantName), returnType, init);
		injectField(fieldNode.up(), fieldConstant);
        System.out.println ("Injected "+fieldNode.up().getName()+"."+constantName);
	}
	
	public static String camelCaseToConstant(final String fieldName) {
		if (fieldName == null || fieldName.isEmpty()) return "";
		char[] chars = fieldName.toCharArray();
		StringBuilder b = new StringBuilder();
		b.append(Character.toUpperCase(chars[0]));
		for (int i = 1, iend = chars.length; i < iend; i++) {
			char c = chars[i];
			if (Character.isUpperCase(c)) {
				b.append('_');
			} else {
				c = Character.toUpperCase(c);
			}
			b.append(c);
		}
		return b.toString();
	}
}