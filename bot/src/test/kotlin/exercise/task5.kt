package exercise

import org.junit.jupiter.api.Test

class FamilyTree(val root: Node) {
    data class Node(val name: String, val yearLifespan: String, var parent: Node?, var right: Node? = null, var left: Node? = null, var deleted: Boolean = false, var height: Int = 0, var subTreeHeight : Int = 0, var originTreeLeft: Node? = null, var originTreeRight: Node? = null)
    val nameTree : NameTree = NameTree(root)
    class NameTree(var root: Node?){
        fun updateHeights(node: Node?, change: Int){
            if (node == null) return
            node.height += change
            updateHeights(node.left, change)
            updateHeights(node.right, change)
        }

        fun getBalance(node: Node?) : Int {
            if (node == null) return 0
            var rightSubTreeHeight = 0
            var leftSubTreeHeight = 0
            if (node.right == null) {
                rightSubTreeHeight = 0
            } else {
                rightSubTreeHeight = node.right!!.subTreeHeight + 1
            }
            if (node.left == null) {
                leftSubTreeHeight = 0
            } else {
                leftSubTreeHeight = node.left!!.subTreeHeight + 1
            }
            return leftSubTreeHeight - rightSubTreeHeight
        }

        fun rotateRight(node: Node) {
            var rightNode = node.right
            var rightLeftNode = rightNode?.left
            var parent = node.parent
            node.parent = rightNode
            node.right = rightLeftNode
            rightLeftNode?.parent = node
            rightNode?.left = node
            rightNode?.parent = parent
            if (parent == null) {
                this.root = rightNode!!
            } else {
                if (parent.left == node) {
                    parent.left = rightNode
                } else {
                    parent.right = rightNode
                }
            }
            node.subTreeHeight = maxOf(node.left?.subTreeHeight ?: 0, node.right?.subTreeHeight ?: 0) + 1
            rightNode?.subTreeHeight = maxOf(rightNode.left?.subTreeHeight ?: 0, rightNode.right?.subTreeHeight ?: 0) + 1
            updateHeights(node, -1)
            updateHeights(rightNode, 1)
        }
        fun rotateLeft(node: Node) {
            var leftNode = node.left
            var leftRightNode = leftNode?.right
            var parent = node.parent
            node.parent = leftNode
            node.left = leftRightNode
            leftRightNode?.parent = node
            leftNode?.right = node
            leftRightNode?.parent = parent
            if (parent == null) {
                this.root = leftNode!!
            } else {
                if (parent.left == node) {
                    parent.left = leftNode
                } else {
                    parent.right = leftNode
                }
            }
            node.subTreeHeight = maxOf(node.left?.subTreeHeight ?: 0, node.right?.subTreeHeight ?: 0) + 1
            leftNode?.subTreeHeight = maxOf(leftNode.left?.subTreeHeight ?: 0, leftNode.right?.subTreeHeight ?: 0) + 1

            updateHeights(node, -1)
            updateHeights(leftNode, 1)
        }

        fun rebalance(node: Node) {
            val balance = getBalance(node)
            val leftbalance = getBalance(node.left)
            val rightbalance = getBalance(node.right)
            if (balance > 1 && rightbalance > 0) {
                rotateLeft(node)
            } else if (balance > 1 && rightbalance < 0) {
                rotateRight(node.right!!)
                rotateLeft(node)
            } else if (balance < -1 && leftbalance < 0) {
                rotateRight(node)
            } else if (balance < -1 && leftbalance > 0) {
                rotateLeft(node.left!!)
                rotateRight(node)
            }
        }

        fun find(name: String): Node? {
            var curNode = this.root ?: return null
            while (curNode.name != name) {
                if (curNode.name > name) {
                    curNode = curNode.left ?: return null
                } else {
                    curNode = curNode.right ?: return null
                }
            }
            return curNode
        }
        fun add(target: Node?, node: Node, height: Int) : Node {
            if (target == null) {
                this.root = node
                node.height = height
                node.parent = null
                updateHeights(node.right, -1)
                updateHeights(node.left, -1)
                return node
            }
            if (target.name > node.name) {
                if (target.left == null) {
                    target.left = node
                    node.parent = target
                    node.height = height + 1
                    if ((node.height + node.subTreeHeight) - target.height > target.subTreeHeight) {
                        target.subTreeHeight =  (node.height + node.subTreeHeight) - target.height
                    }
                    // target.height = 0, node.height = 1, node.subTreeHeight = 1
                    return node
                } else {
                    val res = add(target.left!!, node, height + 1)
                    if (res.height - target.height > target.subTreeHeight) {
                        target.subTreeHeight = res.height - target.height
                    }
                    rebalance(target)
                    return res
                }
            } else {
                if (target.right == null) {
                    target.right = node
                    node.parent = target
                    node.height = height + 1
                    if (node.height - target.height > target.subTreeHeight) {
                        target.subTreeHeight =  node.height - target.height
                    }
                    if ((node.height + node.subTreeHeight) - target.height > target.subTreeHeight) {
                        target.subTreeHeight =  (node.height + node.subTreeHeight) - target.height
                    }
                    return node
                } else {
                    val res = add(target.right!!, node, height + 1)
                    if (res.height - target.height > target.subTreeHeight) {
                        target.subTreeHeight =  res.height - target.height
                    }
                    rebalance(target)
                    return res
                }
            }

        }
        fun delete(name: String){
            var curNode = this.root ?: return
            while (curNode.name != name) {
                if (curNode.name > name) {
                    curNode = curNode.left ?: return
                } else {
                    curNode = curNode.right ?: return
                }
            }
            if (curNode == this.root) {
                this.root = null
            }
            if (curNode.parent?.left == curNode) {
                curNode.parent?.left = null
            } else {
                curNode.parent?.right = null
            }
            curNode.left?.let {add(this.root, it, 0)}
            curNode.right?.let {add(this.root, it, 0)}
        }
    }

    fun find(name: String) : Node? {
        val node = nameTree.find(name)
        return node
    }
    fun delete(name: String) {
        find(name)?.deleted = true
        nameTree.delete(name)
    }
    fun add(name: String, lifespan: String, addToName: String) {
        val parent = find(addToName) ?: throw IllegalArgumentException("Parent node not found")
        val newNode = Node(name, lifespan, parent)
        if (parent.originTreeLeft == null || parent.originTreeLeft!!.deleted) {
            newNode.originTreeLeft = parent.originTreeLeft?.originTreeLeft
            newNode.originTreeRight = parent.originTreeLeft?.originTreeRight
            parent.originTreeLeft = newNode
        } else if (parent.originTreeRight == null || parent.originTreeRight!!.deleted) {
            newNode.originTreeLeft = parent.originTreeRight?.originTreeLeft
            newNode.originTreeRight = parent.originTreeRight?.originTreeRight
            parent.originTreeRight = newNode
        } else {
            throw IllegalArgumentException("Parent node already has two children")
        }
        nameTree.add(nameTree.root, newNode, 0)
    }
}

fun sortString(s: String) : String{
    val codes = s.map { it.code }
    val res = codes.sorted()
    return res.map { it.toChar() }.toString()
}

fun myReverseString(s: String) : String {
    var str = s.toCharArray()
    val range =  if (str.size % 2 != 0) str.size/2 else str.size/2 - 1
    for (i in 0..range) {
        val temp = str[i]
        str[i] = str[str.size - 1 - i]
        str[str.size - 1 - i] = temp
    }
    return str.joinToString("")
}

class FamilyTreeTest {
    @Test
    fun test1(){
        val myTree = FamilyTree(FamilyTree.Node("c", "123", null))
        myTree.add("f", "123", "c")
        myTree.add("g", "123", "c")
        myTree.add("d", "123", "f")
        myTree.add("b", "123", "g")
        myTree.add("a", "123", "f")

        //test correctness of adding nodes to origin tree
        assert(
            myTree.root.name == "c" &&
            myTree.find("c")?.originTreeLeft?.name == "f" &&
            myTree.find("c")?.originTreeRight?.name == "g" &&
            myTree.find("f")?.originTreeLeft?.name == "d" &&
            myTree.find("f")?.originTreeRight?.name == "a" &&
            myTree.find("g")?.originTreeLeft?.name == "b"
        )

        //test correctness of adding nodes to name tree
        assert(
            myTree.nameTree.root?.name == "c" &&
            myTree.find("c")?.left?.name == "b" &&
            myTree.find("c")?.right?.name == "f" &&
            myTree.find("b")?.left?.name == "a" &&
            myTree.find("b")?.right == null &&
            myTree.find("a")?.left == null &&
            myTree.find("a")?.right == null &&
            myTree.find("f")?.left?.name == "d" &&
            myTree.find("f")?.right?.name == "g" &&
            myTree.find("d")?.left == null &&
            myTree.find("d")?.right == null &&
            myTree.find("g")?.left == null &&
            myTree.find("g")?.right == null
        )

        // test correctness of keeping parent nodes, heights and subTreeHeights
        val a = myTree.find("a")
        val b = myTree.find("b")
        val c = myTree.find("c")
        val d = myTree.find("d")
        val f = myTree.find("f")
        val g = myTree.find("g")

        // Parents
        assert(a?.parent?.name == "b") {
            "a.parent: expected=b, actual=${a?.parent?.name}"
        }
        assert(b?.parent?.name == "c") {
            "b.parent: expected=c, actual=${b?.parent?.name}"
        }
        assert(c?.parent == null) {
            "c.parent: expected=null, actual=${c?.parent?.name}"
        }
        assert(d?.parent?.name == "f") {
            "d.parent: expected=f, actual=${d?.parent?.name}"
        }
        assert(f?.parent?.name == "c") {
            "f.parent: expected=c, actual=${f?.parent?.name}"
        }
        assert(g?.parent?.name == "f") {
            "g.parent: expected=f, actual=${g?.parent?.name}"
        }

        // Heights
        assert(a?.height == 2) {
            "a.height: expected=2, actual=${a?.height}"
        }
        assert(b?.height == 1) {
            "b.height: expected=1, actual=${b?.height}"
        }
        assert(c?.height == 0) {
            "c.height: expected=0, actual=${c?.height}"
        }
        assert(d?.height == 2) {
            "d.height: expected=2, actual=${d?.height}"
        }
        assert(f?.height == 1) {
            "f.height: expected=1, actual=${f?.height}"
        }
        assert(g?.height == 2) {
            "g.height: expected=2, actual=${g?.height}"
        }

        // Subtree heights
        assert(a?.subTreeHeight == 0) {
            "a.subTreeHeight: expected=0, actual=${a?.subTreeHeight}"
        }
        assert(b?.subTreeHeight == 1) {
            "b.subTreeHeight: expected=1, actual=${b?.subTreeHeight}"
        }
        assert(c?.subTreeHeight == 2) {
            "c.subTreeHeight: expected=2, actual=${c?.subTreeHeight}"
        }
        assert(d?.subTreeHeight == 0) {
            "d.subTreeHeight: expected=0, actual=${d?.subTreeHeight}"
        }
        assert(f?.subTreeHeight == 1) {
            "f.subTreeHeight: expected=1, actual=${f?.subTreeHeight}"
        }
        assert(g?.subTreeHeight == 0) {
            "g.subTreeHeight: expected=0, actual=${g?.subTreeHeight}"
        }

        //test correctness of origin tree deletion
        //              c
        //           /     \
        //          f       g
        //        /   \    /
        //       d    a   b
        myTree.delete("f")
        myTree.delete("b")
        assert(myTree.find("c")?.originTreeLeft?.deleted == true)
        assert(myTree.find("g")?.originTreeLeft?.deleted == true)
        assert(myTree.find("f") == null)
        assert(myTree.find("b") == null)
        //              c
        //           /     \
        //       deleted    g
        //        /   \    /
        //       d    a  deleted

        //test correctness of name tree deletion
        assert(myTree.root.name == "c" && myTree.root.subTreeHeight == 2 && myTree.root.height == 0)
        val an = myTree.find("a")
        val dn = myTree.find("d")
        val gn = myTree.find("g")
        assert(myTree.root.left?.name == "a" && an?.height == 1 && an.subTreeHeight == 0 && an.left == null && an.right == null && an.parent?.name == "c")
        assert(myTree.root.right?.name == "d" && dn?.height == 1 && dn.subTreeHeight == 1 && dn.left == null && dn.right?.name == "g" && dn.parent?.name == "c")
        assert(gn?.height == 2 && gn.subTreeHeight == 0 && gn.left == null && gn.right == null && gn.parent?.name == "d")



        //test origin tree deleted nodes replacement
        myTree.add("z", "123", "c")
        myTree.add("x", "123", "g")
        assert(myTree.find("c")?.originTreeLeft?.name == "z")
        assert(myTree.find("z")?.originTreeLeft?.name == "d" && myTree.find("z")?.originTreeRight?.name == "a")
        assert(myTree.find("g")?.originTreeLeft?.name == "x")
        //              c
        //           /     \
        //          z       g
        //        /   \    /
        //       d    a   x
    }
    @Test
    fun test2(){
        val myTree = FamilyTree(FamilyTree.Node("c", "123", null))
        myTree.add("f", "123", "c")
        myTree.add("g", "123", "c")
        myTree.add("d", "123", "f")
        myTree.add("b", "123", "g")
        myTree.add("a", "123", "f")


        //test name tree root deletion
        myTree.delete("c")

        val root = myTree.nameTree.root
        val a = myTree.find("a")
        val b = myTree.find("b")
        val d = myTree.find("d")
        val f = myTree.find("f")
        val g = myTree.find("g")

        // Проверка структуры
        assert(root?.name == "b") { "Root should be 'b', but was ${root?.name}" }
        assert(b?.left?.name == "a") { "b.left should be 'a', but was ${b?.left?.name}" }
        assert(b?.right?.name == "f") { "b.right should be 'f', but was ${b?.right?.name}" }
        assert(f?.left?.name == "d") { "f.left should be 'd', but was ${f?.left?.name}" }
        assert(f?.right?.name == "g") { "f.right should be 'g', but was ${f?.right?.name}" }

        // Проверка родителей
        assert(b?.parent == null) { "b.parent should be null" }
        assert(a?.parent?.name == "b") { "a.parent should be 'b'" }
        assert(f?.parent?.name == "b") { "f.parent should be 'b'" }
        assert(d?.parent?.name == "f") { "d.parent should be 'f'" }
        assert(g?.parent?.name == "f") { "g.parent should be 'f'" }

        // Проверка высот нод (height)
        assert(b?.height == 0) { "b.height should be 0, but was ${b?.height}" }
        assert(a?.height == 1) { "a.height should be 1, but was ${a?.height}" }
        assert(f?.height == 1) { "f.height should be 1, but was ${f?.height}" }
        assert(d?.height == 2) { "d.height should be 2, but was ${d?.height}" }
        assert(g?.height == 2) { "g.height should be 2, but was ${g?.height}" }

        // Проверка высот поддеревьев (subTreeHeight)
        assert(b?.subTreeHeight == 2) { "b.subTreeHeight should be 2, but was ${b?.subTreeHeight}" }
        assert(a?.subTreeHeight == 0) { "a.subTreeHeight should be 0, but was ${a?.subTreeHeight}" }
        assert(f?.subTreeHeight == 1) { "f.subTreeHeight should be 1, but was ${f?.subTreeHeight}" }
        assert(d?.subTreeHeight == 0) { "d.subTreeHeight should be 0, but was ${d?.subTreeHeight}" }
        assert(g?.subTreeHeight == 0) { "g.subTreeHeight should be 0, but was ${g?.subTreeHeight}" }
    }
}