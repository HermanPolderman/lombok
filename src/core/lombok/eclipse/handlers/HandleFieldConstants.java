package lombok.eclipse.handlers;

import lombok.AccessLevel;
import lombok.FieldConstants;
import lombok.core.AST.Kind;
import lombok.core.AnnotationValues;
import lombok.eclipse.Eclipse;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.mangosdk.spi.ProviderFor;

import java.lang.reflect.Modifier;
import java.util.Collection;

import static java.lang.Character.isUpperCase;
import static java.lang.Character.toUpperCase;
import static lombok.eclipse.handlers.EclipseHandlerUtil.*;

@ProviderFor(EclipseAnnotationHandler.class) public class HandleFieldConstants extends EclipseAnnotationHandler<FieldConstants> {
	
	public boolean generateFieldDefaultsForType(EclipseNode typeNode, EclipseNode errorNode, AccessLevel level, boolean checkForTypeLevelFieldConstants) {
		
		if (checkForTypeLevelFieldConstants) {
			if (hasAnnotation(FieldConstants.class, typeNode)) {
				return true;
			}
		}
		
		TypeDeclaration typeDecl = null;
		if (typeNode.get() instanceof TypeDeclaration) typeDecl = (TypeDeclaration) typeNode.get();
		
		int modifiers = typeDecl == null ? 0 : typeDecl.modifiers;
		boolean notAClass = (modifiers & (ClassFileConstants.AccInterface | ClassFileConstants.AccAnnotation)) != 0;
		
		if (typeDecl == null || notAClass) {
			errorNode.addError("@FieldConstants is only supported on a class or an enum or a field.");
			return false;
		}
		
		for (EclipseNode field : typeNode.down()) {
			if (fieldQualifiesForFieldConstantsGeneration(field)) generateFieldConstantsForField(field, errorNode.get(), level);
			if (field.getKind() != Kind.FIELD) return false;
		}
		return true;
	}
	
	private void generateFieldConstantsForField(EclipseNode fieldNode, ASTNode pos, AccessLevel level) {
		if (hasAnnotation(FieldConstants.class, fieldNode)) {
			return;
		}
		createFieldConstantsForField(level, fieldNode, fieldNode, pos, false);
	}
	
	private void createFieldConstantsForField(AccessLevel level, EclipseNode fieldNode, EclipseNode errorNode, ASTNode source, boolean whineIfExists) {
		if (fieldNode.getKind() != Kind.FIELD) {
			errorNode.addError("@FieldConstants is only supported on a class or a field");
			return;
		}
		FieldDeclaration field = (FieldDeclaration) fieldNode.get();
		String constantName = camelCaseToConstant(new String(field.name));
		if (constantName == null) {
			errorNode.addWarning("Not generating constant for this field: It does not fit in your @Accessors prefix list");
			return;
		}
		int pS = source.sourceStart, pE = source.sourceEnd;
		long p = (long) pS << 32 | pE;
		FieldDeclaration fieldConstant = new FieldDeclaration(constantName.toCharArray(), pS,pE);
		fieldConstant.bits |= Eclipse.ECLIPSE_DO_NOT_TOUCH_FLAG;
		fieldConstant.modifiers = toEclipseModifier(level) | Modifier.STATIC | Modifier.FINAL;
		fieldConstant.type = new QualifiedTypeReference(TypeConstants.JAVA_LANG_STRING, new long[]{p,p,p});
		fieldConstant.initialization = new StringLiteral(field.name, pS,pE,0);
		injectField(fieldNode.up(), fieldConstant);
	}
	
	private boolean fieldQualifiesForFieldConstantsGeneration(EclipseNode field) {
		if (field.getKind() != Kind.FIELD) return false;
		FieldDeclaration fieldDecl = (FieldDeclaration) field.get();
		return filterField(fieldDecl);
	}
	
	public void handle(AnnotationValues<FieldConstants> annotation, Annotation ast, EclipseNode annotationNode) {
		EclipseNode node = annotationNode.up();
		FieldConstants annotatationInstance = annotation.getInstance();
		AccessLevel level = annotatationInstance.level();
		if (node == null) return;
		switch (node.getKind()){
		case FIELD:
			createFieldConstantsForFields(level, annotationNode.upFromAnnotationToFields(), annotationNode, annotationNode.get(), true);
			break;
		case TYPE:
			generateFieldDefaultsForType(node, annotationNode, level, false);
			break;
			
		}
		
	}
	
	private void createFieldConstantsForFields(AccessLevel level, Collection<EclipseNode> fieldNodes, EclipseNode errorNode, ASTNode source, boolean whineIfExists) {
		for (EclipseNode fieldNode : fieldNodes){
			createFieldConstantsForField(level, fieldNode, errorNode, source, whineIfExists);
		}
	}

	public static String camelCaseToConstant(final String fieldName) {
		if (fieldName == null || fieldName.isEmpty()) return "";
		char[] chars = fieldName.toCharArray();
		StringBuilder b = new StringBuilder();
		b.append(toUpperCase(chars[0]));
		for (int i = 1, iend = chars.length; i < iend; i++) {
			char c = chars[i];
			if (isUpperCase(c)) {
				b.append('_');
			} else {
				c = toUpperCase(c);
			}
			b.append(c);
		}
		return b.toString();
	}
	
}
