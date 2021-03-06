package org.jetbrains.plugin.devkt.clojure.psi.api;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugin.devkt.clojure.psi.ClojurePsiElement;

/**
 * @author ilyas
 */
public interface ClQuotedForm {
	@Nullable
	ClojurePsiElement getQuotedElement();
}
