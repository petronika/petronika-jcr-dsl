package net.petronika.jcr

import javax.jcr.Session
import javax.jcr.Value

/**
 * JCR Constants & Utility Routines
 */
class JcrTools {

	private JcrTools() {
	}

	/**
	 * Namespace delimeter
	 */
	static final String NAMESPACE_DELIMETER = ":"

	/**
	 * Item path separator
	 */
	static final String PATH_SEPARATOR = "/"

	/**
	 * Default node primary type name
	 */
	static final String DEFAULT_PRIMARY_TYPE = "nt:unstructured"

	/**
	 * Creates a JCR value from an object
	 * 
	 * @param session a JCR Session
	 * @param value an object
	 * 
	 * @return a JCR value
	 */
	static Value createValue(Session session, Object value) {
		return session.valueFactory.createValue(value)
	}
}