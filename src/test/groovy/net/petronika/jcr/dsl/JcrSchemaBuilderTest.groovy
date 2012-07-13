package net.petronika.jcr.dsl

import org.junit.*

import static org.junit.Assert.*

import javax.jcr.*
import static javax.jcr.PropertyType.*

import org.apache.jackrabbit.commons.JcrUtils

class JcrSchemaBuilderTest {

	//final static String REPOSITORY_URI = "http://localhost:8080/rmi"
	final static String REPOSITORY_URI = "http://localhost:8080/server"

	static Session session

	@BeforeClass
	static void setUp() {
		Repository repository = JcrUtils.getRepository(REPOSITORY_URI)
		SimpleCredentials credentials = new SimpleCredentials("admin", "admin".toCharArray())
		session = repository.login(credentials)
	}

	@AfterClass
	static void tearDown() {
		session.logout()
	}

	@Test
	void testNamespaceRegistration() {
		JcrSchemaBuilder jcr = new JcrSchemaBuilder(session)

		jcr.registerNamespace("testmix", "http://www.petronika.net/testjcr/mix/1.0")
		jcr.registerNamespace("testjcr", "http://www.petronika.net/testjcr/1.0")
		jcr.registerNamespace("testnt", "http://www.petronika.net/testjcr/nt/1.0")

		// Not supported by Apache Jackrabbit
		//jcr.unregisterNamespace("testmix")
		//jcr.unregisterNamespace("testjcr")
		//jcr.unregisterNamespace("testnt")
	}

	@Test
	void testNodeTypeRegistration() {
		JcrSchemaBuilder jcr = new JcrSchemaBuilder(session)

		jcr.register('testmix', 'testjcr') {
			created {
				MIXIN()
				'-created'(DATE) {
					/*AUTOCREATED();*/ PROTECTED()
				}
				'-createdBy'(STRING) {
					PROTECTED()
				}
			}
			etag {
				MIXIN()
				'-etag'(STRING) {
					DEFAULT_VALUES('')
					AUTOCREATED(); PROTECTED()
				}
			}
			language {
				MIXIN()
				'-language'(STRING)
			}
			lastModified {
				MIXIN()
				'-lastModified'(DATE)
				'-lastModifiedBy'(STRING)
			}
			lifecycle {
				MIXIN()
				'-lifecyclePolicy'(REFERENCE) {
					PROTECTED(); INITIALIZE()
				}
				'-currentLifecycleState'(STRING) {
					PROTECTED(); INITIALIZE()
				}
			}
			lockable {
				MIXIN()
				'-lockOwner'(STRING) {
					PROTECTED(); IGNORE()
				}
				'-lockIsDeep'(BOOLEAN) {
					PROTECTED(); IGNORE()
				}
			}
			mimeType {
				MIXIN()
				'-mimeType'(STRING)
				'-encoding'(STRING)
			}
			referenceable {
				MIXIN()
				'-uuid'(STRING) {
					MANDATORY(); /*AUTOCREATED();*/ PROTECTED(); INITIALIZE()
				}
			}
			shareable('referenceable') {
				MIXIN()
			}
			simpleVersionable {
				MIXIN()
				'-isCheckedOut'(BOOLEAN) {
					DEFAULT_VALUES(true)
					MANDATORY(); /*AUTOCREATED();*/ PROTECTED(); IGNORE()
				}
			}
			title {
				MIXIN()
				'-title'(STRING)
				'-description'(STRING)
			}
			versionable([ 'referenceable', 'simpleVersionable' ]) {
				MIXIN()
				'-versionHistory'(REFERENCE) {
					VALUE_CONSTRAINTS('nt:versionHistory')
					MANDATORY(); PROTECTED(); IGNORE()
				}
				'-baseVersion'(REFERENCE) {
					VALUE_CONSTRAINTS('nt:version')
					MANDATORY(); PROTECTED(); IGNORE()
				}
				'-predecessors'(REFERENCE) {
					VALUE_CONSTRAINTS('nt:version')
					MANDATORY(); PROTECTED(); MULTIPLIE(); IGNORE()
				}
				'-mergeFailed'(REFERENCE) {
					VALUE_CONSTRAINTS('nt:version')
					PROTECTED(); MULTIPLIE(); ABORT()
				}
				'-activity'(REFERENCE) {
					VALUE_CONSTRAINTS('nt:activity')
					PROTECTED(); IGNORE()
				}
				'-configuration'(REFERENCE) {
					VALUE_CONSTRAINTS('nt:configuration')
					PROTECTED(); IGNORE()
				}
			}
		}

		jcr.register('testnt', 'testjcr') {
			activity([ 'nt:base', 'testmix:referenceable' ]) {
				'-activityTitle'(STRING) {
					MANDATORY(); /*AUTOCREATED();*/ PROTECTED()
				}
			}
			address {
				'-protocol'(STRING)
				'-host'(STRING)
				'-port'(STRING)
				'-repository'(STRING)
				'-workspace'(STRING)
				'-path'(PATH)
				'-id'(WEAKREFERENCE)
			}
//			base {
//				'-primaryType'(NAME) {
//					MANDATORY(); /*AUTOCREATED();*/ PROTECTED(); COMPUTE();
//				}
//				'-mixinTypes'(NAME) {
//					PROTECTED(); MULTIPLIE(); COMPUTE()
//				}
//			}
			childNodeDefinition {
				'-name'(NAME) {
					PROTECTED()
				}
				'-autoCreated'() {
					MANDATORY(); PROTECTED()
				}
				'-mandatory'(BOOLEAN) {
					MANDATORY(); PROTECTED()
				}
				'-onParentVersion'(STRING) {
					VALUE_CONSTRAINTS('COPY', 'VERSION', 'INITIALIZE', 'COMPUTE', 'IGNORE', 'ABORT')
					MANDATORY(); PROTECTED()
				}
				'-protected'(BOOLEAN) {
					MANDATORY(); PROTECTED()
				}
				'-requiredPrimaryTypes'(NAME) {
					DEFAULT_VALUES('nt:base')
					MANDATORY(); PROTECTED(); MULTIPLIE()
				}
				'-defaultPrimaryType'(NAME) {
					PROTECTED()
				}
				'-sameNameSiblings'(BOOLEAN) {
					MANDATORY(); PROTECTED()
				}
			}
			configuration([ 'nt:base', 'testmix:versionable' ]) {
				'-root'(REFERENCE) {
					MANDATORY(); /*AUTOCREATED();*/ PROTECTED()
				}
			}
			file('hierarchyNode') {
				PRIMARYITEM('testjcr:content')
				'+testjcr:content'('nt:base') {
					MANDATORY()
				}
			}
			folder('hierarchyNode') {
				'+*'('hierarchyNode') {
					VERSION()
				}
			}
			frozenNode([ 'nt:base', 'testmix:referenceable' ]) {
				ORDERABLE()
				'-frozenPrimaryType'(NAME) {
					MANDATORY(); /*AUTOCREATED();*/ PROTECTED(); ABORT()
				}
				'-frozenMixinTypes'(NAME) {
					PROTECTED(); MULTIPLIE(); ABORT()
				}
				'-frozenUuid'(STRING) {
					MANDATORY(); /*AUTOCREATED();*/ PROTECTED(); ABORT()
				}
				'-*'(UNDEFINED) {
					PROTECTED(); ABORT()
				}
				'-*'(UNDEFINED) {
					PROTECTED(); MULTIPLIE(); ABORT()
				}
				'+*'('nt:base') {
					PROTECTED(); SNS(); ABORT()
				}
			}
			hierarchyNode([ 'nt:base', 'testmix:created' ])
			linkedFile('hierarchyNode') {
				PRIMARYITEM('testjcr:content')
				'-content'(REFERENCE) {
					MANDATORY()
				}
			}
			nodeType {
				'-nodeTypeName'(NAME) {
					MANDATORY(); PROTECTED()
				}
				'-supertypes'(NAME) {
					PROTECTED(); MULTIPLIE()
				}
				'-isAbstract'(BOOLEAN) {
					MANDATORY(); PROTECTED()
				}
				'-isQueryable'(BOOLEAN) {
					MANDATORY(); PROTECTED()
				}
				'-isMixin'(BOOLEAN) {
					MANDATORY(); PROTECTED()
				}
				'-hasOrderableChildNodes'(BOOLEAN) {
					MANDATORY(); PROTECTED()
				}
				'-primaryItemName'(NAME) {
					PROTECTED()
				}
				'+testjcr:propertyDefinition'('propertyDefinition') {
					DEFAULT_TYPE('propertyDefinition')
					PROTECTED(); SNS()
				}
				'+testjcr:childNodeDefinition'('childNodeDefinition') {
					DEFAULT_TYPE('childNodeDefinition')
					PROTECTED(); SNS()
				}
			}
			propertyDefinition {
				'-name'(NAME) {
					PROTECTED()
				}
				'-autoCreated'(BOOLEAN) {
					MANDATORY(); PROTECTED()
				}
				'-mandatory'(BOOLEAN) {
					MANDATORY(); PROTECTED()
				}
				'-onParentVersion'(STRING) {
					VALUE_CONSTRAINTS('COPY', 'VERSION', 'INITIALIZE', 'COMPUTE', 'IGNORE', 'ABORT')
					MANDATORY(); PROTECTED()
				}
				'-protected'(BOOLEAN) {
					MANDATORY(); PROTECTED()
				}
				'-requiredType'(STRING) {
					VALUE_CONSTRAINTS('STRING', 'URI', 'BINARY', 'LONG', 'DOUBLE', 'DECIMAL', 'BOOLEAN', 'DATE', 'NAME', 'PATH', 'REFERENCE', 'WEAKREFERENCE', 'UNDEFINED')
					MANDATORY(); PROTECTED()
				}
				'-valueConstraints'(STRING) {
					PROTECTED(); MULTIPLIE()
				}
				'-defaultValues'(UNDEFINED) {
					PROTECTED(); MULTIPLIE()
				}
				'-MULTIPLIE'(BOOLEAN) {
					MANDATORY(); PROTECTED()
				}
				'-availableQueryOperators'(NAME) {
					MANDATORY(); PROTECTED(); MULTIPLIE()
				}
				'-isFullTextSearchable'(BOOLEAN) {
					MANDATORY(); PROTECTED()
				}
				'-isQueryOrderable'(BOOLEAN) {
					MANDATORY(); PROTECTED()
				}
			}
			query {
				'-statement'(STRING)
				'-language'(STRING)
			}
			resource([ 'nt:base', 'testmix:lastModified', 'testmix:mimeType' ]) {
				PRIMARYITEM('testjcr:data')
				'-data'(BINARY) {
					MANDATORY()
				}
			}
			unstructured {
				ORDERABLE()
				'-*'(UNDEFINED) {
					MULTIPLIE()
				}
				'-*'(UNDEFINED)
				'+*'('nt:base') {
					DEFAULT_TYPE('nt:unstructured')
					SNS(); VERSION()
				}
			}
			version([ 'nt:base', 'testmix:referenceable' ]) {
				'-created'(DATE) {
					MANDATORY(); /*AUTOCREATED();*/ PROTECTED(); ABORT()
				}
				'-predecessors'(REFERENCE) {
					VALUE_CONSTRAINTS('testnt:version')
					PROTECTED(); MULTIPLIE(); ABORT()
				}
				'-successors'(REFERENCE) {
					VALUE_CONSTRAINTS('testnt:version')
					PROTECTED(); MULTIPLIE(); ABORT()
				}
				'-activity'(REFERENCE) {
					VALUE_CONSTRAINTS('testnt:activity')
					PROTECTED(); ABORT()
				}
				'+testjcr:frozenNode'('frozenNode') {
					PROTECTED(); ABORT()
				}
			}
			versionHistory([ 'nt:base', 'testmix:referenceable' ]) {
				'-versionableUuid'(STRING) {
					MANDATORY(); /*AUTOCREATED();*/ PROTECTED(); ABORT()
				}
				'-copiedFrom'(WEAKREFERENCE) {
					VALUE_CONSTRAINTS('testnt:version')
					PROTECTED(); ABORT()
				}
				'+testjcr:rootVersion'('version') {
					DEFAULT_TYPE('version')
					MANDATORY(); /*AUTOCREATED();*/ PROTECTED(); ABORT()
				}
				'+testjcr:versionLabels'('versionLabels') {
					DEFAULT_TYPE('versionLabels')
					MANDATORY(); /*AUTOCREATED();*/ PROTECTED(); ABORT()
				}
				'+*'('version') {
					DEFAULT_TYPE('version')
					PROTECTED(); ABORT()
				}
			}
			versionLabels {
				'-*'(REFERENCE) {
					VALUE_CONSTRAINTS('testnt:version')
					PROTECTED(); ABORT()
				}
			}
			// And finally - variable usage example
			String nodeTypeName = 'versionedChild'
			String nodePropertyName = 'childVersionHistory'
			$(nodeTypeName) {
				$('-' + nodePropertyName, REFERENCE) {
					VALUE_CONSTRAINTS('testnt:versionHistory')
					MANDATORY(); /*AUTOCREATED();*/ PROTECTED(); ABORT()
				}
			}
		}
	}
}