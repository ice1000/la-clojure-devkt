package org.jetbrains.plugin.devkt.clojure;

import kotlin.collections.CollectionsKt;
import org.ice1000.devkt.openapi.AnnotationHolder;
import org.ice1000.devkt.openapi.ColorScheme;
import org.ice1000.devkt.openapi.ExtendedDevKtLanguage;
import org.ice1000.devkt.openapi.util.CompletionElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.plugin.devkt.clojure.parser.ClojureParserDefinition;
import org.jetbrains.plugin.devkt.clojure.psi.api.ClKeyword;
import org.jetbrains.plugin.devkt.clojure.psi.api.ClList;
import org.jetbrains.plugin.devkt.clojure.psi.api.ClVector;
import org.jetbrains.plugin.devkt.clojure.psi.api.symbols.ClSymbol;

import javax.swing.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jetbrains.plugin.devkt.clojure.lexer.ClojureTokenTypes.*;

/**
 * DevKt implementation for la-clojure
 *
 * @author ice1000
 */
public class Clojure<TextAttributes> extends ExtendedDevKtLanguage<TextAttributes> {
	@NotNull
	@Override
	public Icon getIcon() {
		return ClojureIcons.CLOJURE_ICON_16x16;
	}

	private static final TokenSet NUMBERS = TokenSet.create(LONG_LITERAL,
			BIG_INT_LITERAL,
			DOUBLE_LITERAL,
			BIG_DECIMAL_LITERAL,
			RATIO);

	private static final TokenSet OPERATORS = TokenSet.create(SHARP, UP, SHARPUP, TILDA, AT, TILDAAT, QUOTE, BACKQUOTE);
	private static final List<String> cljsPredefined = Arrays.asList("Infinity", "-Infinity");
	private static final List<String> cljsTypes = Arrays.asList("default",
			"nil",
			"object",
			"boolean",
			"number",
			"string",
			"array",
			"function");
	private static final List<String> typeMetaAliases = Arrays.asList("int",
			"ints",
			"long",
			"longs",
			"float",
			"floats",
			"double",
			"doubles",
			"void",
			"short",
			"shorts",
			"boolean",
			"booleans",
			"byte",
			"bytes",
			"char",
			"chars",
			"objects");
	private static final List<String> reserved = Arrays.asList("def",
			"defn",
			"defmethod",
			"defn-",
			"if",
			"do",
			"quote",
			"var",
			"recur",
			"throw",
			"try",
			"catch",
			"finally",
			"monitor-enter",
			"monitor-exit",
			"new",
			"set!",
			"fn*",
			"let*",
			"loop*",
			"letfn*",
			"case*",
			"import*",
			"reify*",
			"deftype*",
			"in-ns",
			"load-file",
			"let",
			"loop",
			"when-let",
			"when-some",
			"if-let",
			"if-some",
			"with-open",
			"when-first",
			"with-redefs",
			"for",
			"doseq",
			"dotimes",
			"with-local-vars");

	private Set<CompletionElement> predefinedCompletions = new HashSet<>();

	public Clojure() {
		super(ClojureLanguage.getInstance(), new ClojureParserDefinition());
		predefinedCompletions.addAll(CollectionsKt.map(reserved, CompletionElement::new));
		predefinedCompletions.addAll(CollectionsKt.map(typeMetaAliases, CompletionElement::new));
		predefinedCompletions.addAll(CollectionsKt.map(cljsPredefined, CompletionElement::new));
		predefinedCompletions.addAll(CollectionsKt.map(cljsTypes, CompletionElement::new));
	}

	@Override
	public @NotNull Set<CompletionElement> getInitialCompletionElementList() {
		return predefinedCompletions;
	}

	@Override
	public @NotNull String getLineCommentStart() {
		return ";";
	}

	@Override
	public boolean satisfies(@NotNull String fileName) {
		return fileName.endsWith(".clj") || fileName.endsWith(".cljs") || fileName.endsWith(".cljc") || fileName.equals(
				"built.boot");
	}

	@Override
	public void annotate(@NotNull PsiElement psiElement, @NotNull AnnotationHolder<? super TextAttributes> annotationHolder, @NotNull ColorScheme<? extends TextAttributes> colorScheme) {
		if (psiElement instanceof ClSymbol) symbol((ClSymbol) psiElement, annotationHolder, colorScheme);
		else if (psiElement instanceof ClKeyword) keyword(((ClKeyword) psiElement), annotationHolder, colorScheme);
	}

	private void keyword(ClKeyword psiElement, AnnotationHolder<? super TextAttributes> annotationHolder, ColorScheme<? extends TextAttributes> colorScheme) {
		annotationHolder.highlight(psiElement, colorScheme.getMetaData());
	}

	private void symbol(ClSymbol symbol, AnnotationHolder<? super TextAttributes> annotationHolder, ColorScheme<? extends TextAttributes> colorScheme) {
		String symbolText = symbol.getText();
		if (reserved.contains(symbolText)) {
			annotationHolder.highlight(symbol, colorScheme.getKeywords());
		} else if (typeMetaAliases.contains(symbolText) || cljsTypes.contains(symbolText)) {
			annotationHolder.highlight(symbol, colorScheme.getUserTypeRef());
		} else if (cljsPredefined.contains(symbolText)) {
			annotationHolder.highlight(symbol, colorScheme.getPredefined());
		} else if (symbol.getParent() instanceof ClVector) {
			ClList parent = PsiTreeUtil.getParentOfType(symbol, ClList.class);
			if (null == parent) return;
			ClSymbol functionName = parent.getFirstSymbol();
			if (null == functionName) return;
			PsiElement nextVisibleLeaf = PsiTreeUtil.getNextSiblingOfType(functionName, ClVector.class);
			if (null == nextVisibleLeaf) return;
			if ("let".equals(functionName.getText()) && PsiTreeUtil.isAncestor(nextVisibleLeaf, symbol, true))
				annotationHolder.highlight(symbol, colorScheme.getVariable());
		}
	}

	@Override
	public @Nullable TextAttributes attributesOf(@NotNull IElementType iElementType, @NotNull ColorScheme<? extends TextAttributes> colorScheme) {
		if (iElementType == COMMA) return colorScheme.getComma();
		else if (iElementType == CHAR_LITERAL) return colorScheme.getCharLiteral();
		else if (iElementType == LINE_COMMENT) return colorScheme.getLineComments();
		else if (iElementType == BAD_CHARACTER) return colorScheme.getUnknown();
		else if (iElementType == WRONG_STRING_LITERAL) return colorScheme.getUnknown();
		else if (iElementType == COLON_SYMBOL) return colorScheme.getMetaData();
		else if (iElementType == LEFT_PAREN) return colorScheme.getParentheses();
		else if (iElementType == RIGHT_PAREN) return colorScheme.getParentheses();
		else if (iElementType == LEFT_SQUARE) return colorScheme.getBrackets();
		else if (iElementType == RIGHT_SQUARE) return colorScheme.getBrackets();
		else if (iElementType == LEFT_CURLY) return colorScheme.getBraces();
		else if (iElementType == RIGHT_CURLY) return colorScheme.getBraces();
		else if (NUMBERS.contains(iElementType)) return colorScheme.getNumbers();
		else if (OPERATORS.contains(iElementType)) return colorScheme.getOperators();
		else if (STRINGS.contains(iElementType)) return colorScheme.getString();
		else if (KEYWORDS.contains(iElementType)) return colorScheme.getKeywords();
		return null;
	}
}
