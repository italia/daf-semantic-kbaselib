package check.jgit

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.util.FS
import java.io.File
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.treewalk.CanonicalTreeParser

object MainGit extends App {

  val dir = new File("C:/Users/Al.Serafini/repos/DAF/daf-ontologie-vocabolari-controllati")
  val git = Git.open(dir, FS.detect())
  git.apply()

  // updating...
  git.pull()

  list(dir)
    .filter(_.toString().endsWith(".ttl"))
    .foreach { f =>
      println(f)
    }

  def list(item: File): Seq[File] = {

    item match {

      case file if (file.isFile()) => Stream(file)

      case dir if (dir.isDirectory()) =>
        item #:: dir.listFiles().toStream.flatMap(el => list(el)).toStream

    }

  }

  //  REVIEW HERE
  //  val tree = new CanonicalTreeParser()
  //  val treeWalk = new TreeWalk(git.getRepository)
  //  treeWalk.addTree(tree);
  //  treeWalk.setRecursive(false);
  //
  //  while (treeWalk.next()) {
  //    if (treeWalk.isSubtree()) {
  //      println("dir: " + treeWalk.getPathString());
  //      treeWalk.enterSubtree();
  //    } else {
  //      println("file: " + treeWalk.getPathString());
  //    }
  //  }

}