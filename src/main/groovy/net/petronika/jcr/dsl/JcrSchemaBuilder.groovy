package net.petronika.jcr.dsl

import javax.jcr.*
import javax.jcr.nodetype.*
import javax.jcr.version.OnParentVersionAction

import org.apache.jackrabbit.commons.cnd.CndImporter

import groovy.util.BuilderSupport

import net.petronika.jcr.JcrTools

/**
 * TODO: JavaDoc
 */
class JcrSchemaBuilder extends BuilderSupport {

	static final String ANY_NAME = "*"

	static final String PROPERTY_DEF_PREFIX = "-"
	static final String CHILD_NODE_DEF_PREFIX = "+"

	private Session session = null
	private NamespaceRegistry namespaceRegistry = null
	private NodeTypeManager nodeTypeManager = null

	private List<NodeTypeDefinition> ntds = null
	private String nodeDefaultNamespace = null
	private String propertyDefaultNamespace = null

	/*
	 * -------------------------------------------------------------------------------------
	 * Construction
	 * -------------------------------------------------------------------------------------
	 */

	/**
	 * TODO: JavaDoc
	 * 
	 * @param session
	 */
	JcrSchemaBuilder(Session session) {
		super()
		assert session
		this.session = session
		namespaceRegistry = session.workspace.namespaceRegistry
		nodeTypeManager = session.workspace.nodeTypeManager
	}

	/*
	 * -------------------------------------------------------------------------------------
	 * BuilderSupport overrides
	 * -------------------------------------------------------------------------------------
	 */

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object createNode(Object name) {
		return createNodeInternal(name, null)
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object createNode(Object name, Object value) {
		return createNodeInternal(name, value)
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object createNode(Object name, Map attributes) {
		throw new UnsupportedOperationException("Attribute map are not supported")
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object createNode(Object name, Map attributes, Object value) {
		throw new UnsupportedOperationException("Attribute map are not supported")
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void setParent(Object parent, Object child) {
	}

	/**
	 * TODO: JavaDoc
	 * 
	 * @param name
	 * @param value
	 * @return
	 */
	protected Object createNodeInternal(String name, Object value) {
		assertRegister()
		if ( !getCurrent() ) {
			String[] superTypes
			if ( value != null ) {
				if ( value instanceof String ) {
					superTypes = [ applyNamespace(value) ]
				}
				else if ( value instanceof String[] ) {
					superTypes = value.collect { applyNamespace(it) }
				}
				else if ( value instanceof List ) {
					superTypes = value.collect { applyNamespace(it.toString()) }
				}
				else {
					throw new IllegalArgumentException("The node declared super type names must be one the following types: String, String[], List")
				}
			}
			NodeTypeTemplate ntt = nodeTypeManager.createNodeTypeTemplate()
			ntt.name = applyNamespace(name)
			if ( superTypes ) {
				ntt.declaredSuperTypeNames = superTypes
			}
			ntds << ntt
			return ntt
		}
		if ( getCurrent() instanceof NodeTypeTemplate ) {
			if ( name.length() > 1 ) {
				String prefix = name[0]
				String realName = name[1..-1]
				if ( prefix == PROPERTY_DEF_PREFIX ) {
					if ( realName != ANY_NAME ) {
						realName = applyNamespace(realName, true)
					}
					if ( value != null ) {
						if ( !(value instanceof Integer) ) {
							throw new IllegalArgumentException("The property type must be integer")
						}
					}
					PropertyDefinitionTemplate pdt = nodeTypeManager.createPropertyDefinitionTemplate()
					pdt.name = realName
					if ( value != null ) {
						pdt.requiredType = value
					}
					getCurrent().propertyDefinitionTemplates << pdt
					return pdt
				}
				else if ( prefix == CHILD_NODE_DEF_PREFIX ) {
					if ( realName != ANY_NAME ) {
						realName = applyNamespace(realName, false)
					}
					String[] requiredTypes
					if ( value != null ) {
						if ( value instanceof String ) {
							requiredTypes = [ applyNamespace(value) ]
						}
						else if ( value instanceof String[] ) {
							requiredTypes = value.collect { applyNamespace(it) }
						}
						else if ( value instanceof List ) {
							requiredTypes = value.collect { applyNamespace(it.toString()) }
						}
						else {
							throw new IllegalArgumentException("The child node required primary type names must be one the following types: String, String[], List")
						}
					}
					NodeDefinitionTemplate ndt = nodeTypeManager.createNodeDefinitionTemplate()
					ndt.name = realName
					if ( requiredTypes ) {
						ndt.requiredPrimaryTypeNames = requiredTypes
					}
					getCurrent().nodeDefinitionTemplates << ndt
					return ndt
				}
			}
		}
		throw new IllegalStateException("Name of the component of the node type definition is incorrect: $name")
	}

	/*
	 * -------------------------------------------------------------------------------------
	 * Item definition creation methods
	 * -------------------------------------------------------------------------------------
	 */

	Object $(String name) {
		return invokeMethod(name)
	}

	Object $(String name, Closure closure) {
		return invokeMethod(name, closure)
	}

	Object $(String name, Object params) {
		return invokeMethod(name, params)
	}

	Object $(String name, Object params, Closure closure) {
		return invokeMethod(name, [params, closure])
	}

	/* 
	 * -------------------------------------------------------------------------------------
	 * Registrators
	 * -------------------------------------------------------------------------------------
	 */

	/**
	 * Registers nodetypes in cnd format.
	 * 
	 * @param cnd a reader to the cnd. The reader is closed on return.
	 * @param reregisterExisting true if existing node types should be re-registered with those present in the cnd. false otherwise.
	 * 
	 * @see org.apache.jackrabbit.commons.cnd.CndImporter#registerNodeTypes(Reader, Session, boolean)
	 */
	void registerCND(Reader cnd, boolean reregisterExisting = true) {
		assertNotRegister()
		CndImporter.registerNodeTypes(cnd, session, reregisterExisting)
	}

	/**
	 * Sets a one-to-one mapping between prefix and uri in the global namespace registry of this repository.
	 * 
	 * @param prefix The prefix to be mapped.
	 * @param uri The URI to be mapped.
	 * 
	 * @see javax.jcr.NamespaceRegistry#registerNamespace(String, String)
	 */
	void registerNamespace(String prefix, String uri) {
		assertNotRegister()
		namespaceRegistry.registerNamespace(prefix, uri)
	}

	/**
	 * Removes a namespace mapping from the registry.
	 * 
	 * @param prefix The prefix of the mapping to be removed.
	 * 
	 * @see javax.jcr.NamespaceRegistry#unregisterNamespace(String)
	 */
	void unregisterNamespace(String prefix) {
		assertNotRegister()
		namespaceRegistry.unregisterNamespace(prefix)
	}

	void register(String nodeDefaultNamespace, String propertyDefaultNamespace, boolean allowUpdate, Closure closure) {
		assertNotRegister()
		Object oldDelegate = closure.delegate
		closure.delegate = this
		ntds = new ArrayList<NodeTypeDefinition>()
		this.nodeDefaultNamespace = nodeDefaultNamespace
		this.propertyDefaultNamespace = propertyDefaultNamespace
		try {
			closure.call()
			if ( ntds.size() ) {
				nodeTypeManager.registerNodeTypes(ntds as NodeTypeDefinition[], allowUpdate)
			}
		}
		finally {
			closure.delegate = oldDelegate
			ntds = null
			this.nodeDefaultNamespace = null
			this.propertyDefaultNamespace = null
		}
	}

	void register(String nodeDefaultNamespace, String propertyDefaultNamespace, Closure closure) {
		register(nodeDefaultNamespace, propertyDefaultNamespace, true, closure)
	}

	void register(String defaultNamespace, boolean allowUpdate, Closure closure) {
		register(defaultNamespace, defaultNamespace, allowUpdate, closure)
	}

	void register(String defaultNamespace, Closure closure) {
		register(defaultNamespace, true, closure)
	}

	void register(boolean allowUpdate, Closure closure) {
		register(null, allowUpdate, closure)
	}

	void register(Closure closure) {
		register(null, true, closure)
	}

	/**
	 * Unregisters the specified node type.
	 * 
	 * @param name The name of the node type to be unregister.
	 * 
	 * @see javax.jcr.nodetype.NodeTypeManager#unregisterNodeType(String)
	 */
	void unregister(String name) {
		assertNotRegister()
		nodeTypeManager.unregisterNodeType(name)
	}

	/**
	 * Unregisters the specified set of node types.
	 * 
	 * @param names The array of names of the node types to be unregister.
	 * 
	 * @see javax.jcr.nodetype.NodeTypeManager#unregisterNodeTypes(String[])
	 */
	void unregister(String[] names) {
		assertNotRegister()
		nodeTypeManager.unregisterNodeTypes(names)
	}

	/*
	 * -------------------------------------------------------------------------------------
	 * Node type attributes
	 * -------------------------------------------------------------------------------------
	 */

	/**
	 * Sets the orderable child nodes flag of the node type.
	 * 
	 * @param orderable a flag
	 */
	void ORDERABLE(boolean orderable = true) {
		assertNodeType()
		getCurrent().orderableChildNodes = orderable
	}

	/**
	 * Sets the mixin flag of the node type.
	 * 
	 * @param mixin a flag
	 */
	void MIXIN(boolean mixin = true) {
		assertNodeType()
		getCurrent().mixin = mixin
	}

	/**
	 * Sets the abstract flag of the node type.
	 * 
	 * @param abstractStatus a flag
	 */
	void ABSTRACT(boolean abstractStatus = true) {
		assertNodeType()
		getCurrent().setAbstract(abstractStatus)
	}

	/**
	 * Sets the queryable status of the node type.
	 * 
	 * @param queryable a flag
	 */
	void QUERY(boolean queryable = true) {
		assertNodeType()
		getCurrent().queryable = queryable
	}

	/**
	 * Sets the name of the primary item.
	 * 
	 * @param name A JCR name
	 */
	void PRIMARYITEM(String name) {
		assertNodeType()
		getCurrent().primaryItemName = applyNamespace(name)
	}

	/*
	 * -------------------------------------------------------------------------------------
	 * Property and child node attributes
	 * -------------------------------------------------------------------------------------
	 */

	/**
	 * Sets the default value of the property.
	 * 
	 * @param value a value.
	 */
	void DEFAULT_VALUES(Object value) {
		assertPropertyDef()
		getCurrent().defaultValues = [ JcrTools.createValue(session, value) ]
	}

	/**
	 * Sets the default values of the multi-value property.
	 * 
	 * @param values a value array.
	 */
	void DEFAULT_VALUES(Object[] values) {
		assertPropertyDef()
		getCurrent().defaultValues = values.collect() { JCRUtils.createValue(session, it) }
	}

	/**
	 * Sets the default values of the multi-value property.
	 * 
	 * @param values a value list.
	 */
	void DEFAULT_VALUES(List<Object> values) {
		assertPropertyDef()
		getCurrent().defaultValues = values.collect() { JCRUtils.createValue(session, it) }
	}

	/**
	 * Sets the value constraint of the property.
	 * 
	 * @param constraint a constraint.
	 */
	void VALUE_CONSTRAINTS(String constraint) {
		assertPropertyDef()
		getCurrent().valueConstraints = [ constraint ]
	}

	/**
	 * Sets the value constraints of the property.
	 * 
	 * @param constraints a constraint array.
	 */
	void VALUE_CONSTRAINTS(String[] constraints) {
		assertPropertyDef()
		getCurrent().valueConstraints = constraints
	}

	/**
	 * Sets the value constraints of the property.
	 * 
	 * @param constraints a constraint list.
	 */
	void VALUE_CONSTRAINTS(List<String> constraints) {
		assertPropertyDef()
		getCurrent().valueConstraints = constraints
	}

	/**
	 * Sets the auto-create status of the property or the child node.
	 * 
	 * @param autoCreated a flag
	 */
	void AUTOCREATED(boolean autoCreated = true) {
		assertPropertyDefOrNodeDef()
		getCurrent().autoCreated = autoCreated
	}

	/**
	 * Sets the mandatory status of the property or the child node.
	 * 
	 * @param mandatory a flag
	 */
	void MANDATORY(boolean mandatory = true) {
		assertPropertyDefOrNodeDef()
		getCurrent().mandatory = mandatory
	}

	/**
	* Sets the protected status of the property or the child node.
	*
	* @param protectedStatus a flag
	*/
	void PROTECTED(boolean protectedStatus = true) {
		assertPropertyDefOrNodeDef()
		getCurrent().setProtected(protectedStatus)
	}

	/**
	* Sets the on-parent-version status of the property or the child node to
	* {@link javax.jcr.version.OnParentVersionAction#COPY} value.
	* 
	* @param opv an integer constant
	* @see javax.jcr.version.OnParentVersionAction
	*/
	void COPY() {
		assertPropertyDefOrNodeDef()
		getCurrent().onParentVersion = OnParentVersionAction.COPY
	}

	/**
	* Sets the on-parent-version status of the property or the child node to
	* {@link javax.jcr.version.OnParentVersionAction#VERSION} value.
	* 
	* @param opv an integer constant
	* @see javax.jcr.version.OnParentVersionAction
	*/
	void VERSION() {
		assertPropertyDefOrNodeDef()
		getCurrent().onParentVersion = OnParentVersionAction.VERSION
	}

	/**
	* Sets the on-parent-version status of the property or the child node to
	* {@link javax.jcr.version.OnParentVersionAction#INITIALIZE} value.
	* 
	* @param opv an integer constant
	* @see javax.jcr.version.OnParentVersionAction
	*/
	void INITIALIZE() {
		assertPropertyDefOrNodeDef()
		getCurrent().onParentVersion = OnParentVersionAction.INITIALIZE
	}

	/**
	* Sets the on-parent-version status of the property or the child node to
	* {@link javax.jcr.version.OnParentVersionAction#COMPUTE} value.
	* 
	* @param opv an integer constant
	* @see javax.jcr.version.OnParentVersionAction
	*/
	void COMPUTE() {
		assertPropertyDefOrNodeDef()
		getCurrent().onParentVersion = OnParentVersionAction.COMPUTE
	}

	/**
	* Sets the on-parent-version status of the property or the child node to
	* {@link javax.jcr.version.OnParentVersionAction#IGNORE} value.
	* 
	* @param opv an integer constant
	* @see javax.jcr.version.OnParentVersionAction
	*/
	void IGNORE() {
		assertPropertyDefOrNodeDef()
		getCurrent().onParentVersion = OnParentVersionAction.IGNORE
	}

	/**
	* Sets the on-parent-version status of the property or the child node to
	* {@link javax.jcr.version.OnParentVersionAction#ABORT} value.
	* 
	* @param opv an integer constant
	* @see javax.jcr.version.OnParentVersionAction
	*/
	void ABORT() {
		assertPropertyDefOrNodeDef()
		getCurrent().onParentVersion = OnParentVersionAction.ABORT
	}

	/**
	* Sets the multi-value status of the property.
	* 
	* @param multiple a flag
	*/
	void MULTIPLIE(boolean multiple = true) {
		assertPropertyDef()
		getCurrent().multiple = multiple
	}

	/**
	* Sets the set of query comparison operators supported by this property. 
	* 
	* @param operators an array of String constants.
	* @see javax.jcr.nodetype.PropertyDefinition#getAvailableQueryOperators()
	*/
	void QUERYOPS(String[] operators) {
		assertPropertyDef()
		getCurrent().availableQueryOperators = operators
	}

	/**
	* Sets the full-text-searchable status of the property.
	* 
	*  @param noFullText a flag
	*/
	void NOFULLTEXT(boolean noFullText = true) {
		assertPropertyDef()
		getCurrent().fullTextSearchable = !noFullText
	}

	/**
	 * Sets the query-orderable status of the property. 
	 * 
	 * @param noQueryOrder a flag
	 */
	void NOQUERYORDER(boolean noQueryOrder = true) {
		assertPropertyDef()
		getCurrent().queryOrderable = !noQueryOrder
	}

	/**
	 * Sets the same-name sibling status of this node.
	 * 
	 * @param allowSameNameSiblings a flag
	 */
	void SNS(boolean allowSameNameSiblings = true) {
		assertNodeDef()
		getCurrent().sameNameSiblings = allowSameNameSiblings
	}

	/**
	 * Sets the name of the default primary type of this node.
	 * 
	 * @param typeName a JCR name
	 */
	void DEFAULT_TYPE(String typeName) {
		assertNodeDef()
		getCurrent().defaultPrimaryTypeName = applyNamespace(typeName)
	}

	/*
	 * -------------------------------------------------------------------------------------
	 * State checkers
	 * -------------------------------------------------------------------------------------
	 */

	/**
	 * TODO: JavaDoc
	 */
	protected void assertNotRegister() {
		if ( ntds != null ) {
			throw new IllegalStateException("The method call only allowed outside the register closure")
		}
	}

	/**
	 * TODO: JavaDoc
	 */
	protected void assertRegister() {
		if ( ntds == null ) {
			throw new IllegalStateException("The method call only allowed within the register closure")
		}
	}

	/**
	 * TODO: JavaDoc
	 */
	protected void assertNodeType() {
		if ( !(getCurrent() instanceof NodeTypeTemplate) ) {
			throw new IllegalStateException("The method call only allowed within a node type closure")
		}
	}

	/**
	 * TODO: JavaDoc
	 */
	protected void assertPropertyDef() {
		if ( !(getCurrent() instanceof PropertyDefinitionTemplate) ) {
			throw new IllegalStateException("The method call only allowed within a property closure")
		}
	}

	/**
	 * TODO: JavaDoc
	 */
	protected void assertNodeDef() {
		if ( !(getCurrent() instanceof NodeDefinitionTemplate) ) {
			throw new IllegalStateException("The method call only allowed within a child node closure")
		}
	}

	/**
	 * TODO: JavaDoc
	 */
	protected void assertPropertyDefOrNodeDef() {
		if ( !(getCurrent() instanceof PropertyDefinitionTemplate || getCurrent() instanceof NodeDefinitionTemplate) ) {
			throw new IllegalStateException("The method call only allowed within a property closure or within a child node closure")
		}
	}

	/*
	 * -------------------------------------------------------------------------------------
	 * Utilities
	 * -------------------------------------------------------------------------------------
	 */

	protected String applyNamespace(String name, boolean isProperty = false) {
		String defaultNamespace = isProperty ? propertyDefaultNamespace : nodeDefaultNamespace
		if ( defaultNamespace && name.indexOf(JcrTools.NAMESPACE_DELIMETER) == -1 ) {
			name = defaultNamespace + JcrTools.NAMESPACE_DELIMETER + name
		}
		return name
	}
}