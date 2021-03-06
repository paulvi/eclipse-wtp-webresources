/**
 *  Copyright (c) 2013-2014 Angelo ZERR.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package org.eclipse.wst.html.webresources.internal.ui.contentassist;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSStyleRule;
import org.eclipse.wst.html.webresources.core.AbstractCSSClassNameOrIdTraverser;
import org.eclipse.wst.html.webresources.core.WebResourcesFinderType;
import org.eclipse.wst.html.webresources.core.utils.DOMHelper;
import org.eclipse.wst.html.webresources.internal.ui.utils.HTMLWebResourcesPrinter;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest;

/**
 * CSS traverser implementation for CSS class name and CSS id completion.
 */
public class CSSContentAssistTraverser extends
		AbstractCSSClassNameOrIdTraverser {

	private final ContentAssistRequest contentAssistRequest;
	private String matchingClassNameOrId;
	private int replacementOffset;
	private int replacementLength;
	private final List<String> existingClassNames;

	public CSSContentAssistTraverser(ContentAssistRequest contentAssistRequest,
			int documentPosition, String attrValue, WebResourcesFinderType type) {
		super((IDOMNode) contentAssistRequest.getNode(), type);
		this.contentAssistRequest = contentAssistRequest;
		this.existingClassNames = type == WebResourcesFinderType.CSS_CLASS_NAME ? new ArrayList<String>()
				: null;
		// ex : <input class="todo-done myClass" />
		// completion done after my (todo-done my // Here Ctrl+Space)

		// matching string = "todo-done my"
		String matchingString = DOMHelper.getAttrValue(contentAssistRequest
				.getMatchString());

		// matching class name = "my"
		matchingClassNameOrId = matchingString;
		if (type == WebResourcesFinderType.CSS_CLASS_NAME) {
			int index = matchingClassNameOrId.lastIndexOf(" ");
			if (index != -1) {
				matchingClassNameOrId = matchingClassNameOrId.substring(
						index + 1, matchingClassNameOrId.length());
			}

			// after matching class name = "Class"
			String afterMatchingClassName = attrValue.substring(
					matchingString.length(), attrValue.length());
			index = afterMatchingClassName.indexOf(" ");
			if (index != -1) {
				afterMatchingClassName = afterMatchingClassName.substring(0,
						index);
			}
			this.replacementLength = matchingClassNameOrId.length()
					+ afterMatchingClassName.length();

			// compute existing class names
			String[] names = attrValue.split(" ");
			for (int i = 0; i < names.length; i++) {
				existingClassNames.add(names[i].trim());
			}
		} else {
			this.replacementLength = attrValue.length();
		}
		this.replacementOffset = documentPosition
				- matchingClassNameOrId.length();

	}

	@Override
	protected boolean collect(String className, ICSSStyleRule rule) {
		// check if visited class name or id starts with matching class name?
		if (!className.startsWith(matchingClassNameOrId)) {
			return false;
		}
		if (existingClassNames != null
				&& existingClassNames.contains(className)) {
			// CSS class name already exists in the @class attribute, ignore it.
			return false;
		}

		// Compute the display string of the completion proposal
		IDOMNode node = getNode();
		String info = HTMLWebResourcesPrinter.getAdditionalProposalInfo(rule,
				getWebResourcesType(), node);
		String fileName = DOMHelper.getFileName(rule, node);
		String displayString = fileName != null ? new StringBuilder(className)
				.append(" - ").append(fileName).toString() : className;

		int cursorPosition = className.length();
		Image image = HTMLWebResourcesPrinter.getImage(getWebResourcesType());
		// Add CSS class name or id completion proposal
		contentAssistRequest.addProposal(new CSSWebResourcesCompletionProposal(
				className, replacementOffset, replacementLength,
				cursorPosition, image, displayString, null, info));
		// add the class name in the list of class names to avoid having several
		// times the same class name in the completion.
		// see https://github.com/angelozerr/eclipse-wtp-webresources/issues/28
		existingClassNames.add(className);
		return false;
	}

}