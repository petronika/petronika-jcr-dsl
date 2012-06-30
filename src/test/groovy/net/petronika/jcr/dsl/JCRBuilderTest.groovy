package net.petronika.jcr.dsl

import org.junit.BeforeClass
import org.junit.AfterClass
import org.junit.Test
import static org.junit.Assert.*

import javax.jcr.*

import org.apache.jackrabbit.commons.JcrUtils

class JCRBuilderTest {

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
	void testCreation() {
		JCRBuilder jcr = new JCRBuilder(session)

		Node root = session.rootNode

		String nodeName
		Node node

		assertNodesEquals root, jcr.$root(), jcr.$('/'), jcr.'/'(), jcr.$('#' + root.identifier)

		jcr.'test00'()
		jcr.test01()
		jcr.test02('nt:unstructured')
		jcr.test03('nt:unstructured', [ myProperty1: 1, myProperty2: 2 ])
		jcr.test04('nt:unstructured', myProperty1: 1, myProperty2: 2)
		jcr.test05(myProperty1: 1, myProperty2: 2)
		assertNodesEquals root.getNode('test00'), jcr.test00(), jcr.$('test00'), jcr.$('/test00'), jcr.'test00'(), jcr.'/test00'()
		assertNodesEquals root.getNode('test01'), jcr.test01(), jcr.$('test01'), jcr.$('/test01'), jcr.'test01'(), jcr.'/test01'()
		assertNodesEquals root.getNode('test02'), jcr.test02(), jcr.$('test02'), jcr.$('/test02'), jcr.'test02'(), jcr.'/test02'()
		assertNodesEquals root.getNode('test03'), jcr.test03(), jcr.$('test03'), jcr.$('/test03'), jcr.'test03'(), jcr.'/test03'()
		assertNodesEquals root.getNode('test04'), jcr.test04(), jcr.$('test04'), jcr.$('/test04'), jcr.'test04'(), jcr.'/test04'()
		assertNodesEquals root.getNode('test05'), jcr.test05(), jcr.$('test05'), jcr.$('/test05'), jcr.'test05'(), jcr.'/test05'()

		nodeName = 'test06'
		jcr.$(nodeName)
		nodeName = '/test07'
		jcr.$(nodeName, 'nt:unstructured')
		nodeName = 'test08'
		jcr.$(nodeName, 'nt:unstructured', [ myProperty1: 1, myProperty2: 2 ])
		nodeName = '/test09'
		jcr.$(nodeName, 'nt:unstructured', myProperty1: 1, myProperty2: 2)
		nodeName = 'test10'
		jcr.$(nodeName, myProperty1: 1, myProperty2: 2)
		assertNodesEquals root.getNode('test06'), jcr.test06(), jcr.$('test06'), jcr.$('/test06'), jcr.'test06'(), jcr.'/test06'()
		assertNodesEquals root.getNode('test07'), jcr.test07(), jcr.$('test07'), jcr.$('/test07'), jcr.'test07'(), jcr.'/test07'()
		assertNodesEquals root.getNode('test08'), jcr.test08(), jcr.$('test08'), jcr.$('/test08'), jcr.'test08'(), jcr.'/test08'()
		assertNodesEquals root.getNode('test09'), jcr.test09(), jcr.$('test09'), jcr.$('/test09'), jcr.'test09'(), jcr.'/test09'()
		assertNodesEquals root.getNode('test10'), jcr.test10(), jcr.$('test10'), jcr.$('/test10'), jcr.'test10'(), jcr.'/test10'()

		jcr.level0 {
			'level1' {
				$('level2') {
					level3('nt:unstructured') {
						level4('nt:unstructured', myProperty1: 1, myProperty2: 2) {
							level5(myProperty1: 1, myProperty2: 2) {
								node = level6()
								assertNodesEquals(node,
										level6(),
										'level6'(),
										'/level0/level1/level2/level3/level4/level5/level6'(),
										$('/level0/level1/level2/level3/level4/level5/level6'))
							}
						}
					}
				}
			}
		}
		assertNodesEquals(
				node,
				session.getNode('/level0/level1/level2/level3/level4/level5/level6'),
				root.getNode('level0/level1/level2/level3/level4/level5/level6'),
				jcr.$nodeByPath('/level0/level1/level2/level3/level4/level5/level6'),
				jcr.$('/level0/level1/level2/level3/level4/level5/level6'),
				jcr.$('level0/level1/level2/level3/level4/level5/level6'))

		assertTrue session.nodeExists('/level0/level1/level2/level3/level4/level5/level6')
		assertTrue jcr.$nodeExists('/level0/level1/level2/level3/level4/level5/level6')
		assertFalse session.nodeExists('/level0/level1/level2/level3/level4/level5/levelX')
		assertFalse jcr.$nodeExists('/level0/level1/level2/level3/level4/level5/levelX')

		jcr.$save()
	}

	@Test
	void testQuerying() {
		JCRBuilder jcr = new JCRBuilder(session)

		Node root = session.rootNode

		assertNotNull jcr.$findByName('level3')
		assertNotNull jcr.$findByName('level3', 'nt:unstructured')
		assertNotNull jcr.$findByName('level3', 'nt:unstructured', root)
		assertNotNull jcr.$findByName('level3', root)
		assertNull jcr.$findByName('level3', 'nt:unstructured', jcr.$('/level0/level1/level2/level3/level4'))
		assertNull jcr.$findByName('level3', jcr.$('/level0/level1/level2/level3/level4'))

		assertEquals(
				(jcr.$queryNodes("select * from [nt:unstructured] where myProperty1 = 1").collect { it.name }).sort(),
				['level4', 'level5', 'test03', 'test04', 'test05', 'test08', 'test09', 'test10'])
	}

	static void assertNodesEquals(Node... node) {
		Node firstNode = node[0]
		node.each { Node n ->
			assertEquals n.identifier, firstNode.identifier
		}
	}
}