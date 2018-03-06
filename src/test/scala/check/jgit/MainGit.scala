package check.jgit

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.util.FS
import java.io.File
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.merge.MergeStrategy
import org.eclipse.jgit.util.FileUtils
import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.FileVisitResult
import java.io.IOException
import java.nio.file.Paths

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import java.nio.file.FileVisitOption
import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.nio.file.FileVisitor
import scala.util.Try
import scala.util.Success
import scala.util.Failure

/*
 * CHECKING git repo synch + .git deletion
 */
object MainGit extends App {

  val git_remote = "daf-ontologie"
  val path_remote = "https://github.com/italia/daf-ontologie-vocabolari-controllati/"
  val path_local = "D://DAF_ontologie-vocabolari-controllati"
  val git_dir = Paths.get(path_local).normalize()

  println("USING " + git_dir)

  // CHECK?
  //  val dir = new File("C:/Users/Al.Serafini/repos/DAF/kataLOD/ontologie-vocabolari-controllati")
  //  val git = Git.open(dir, FS.detect())
  //  git.apply()

  val git_repo = Git.init().setDirectory(git_dir.toFile()).call()

  // adding the remote repository reference
  val remote_add = git_repo.remoteAdd()
  remote_add.setName(git_remote)
  remote_add.setUri(new URIish(path_remote))
  remote_add.call()

  // $ git clean -fd
  git_repo.clean()
    .setForce(true)
    .setCleanDirectories(true)
    .call()

  val default_branch = "master"

  // download last version
  try {
    // $ git pull daf-ontologie master
    git_repo.pull()
      .setRemote(git_remote)
      .setRemoteBranchName(default_branch)
      .setStrategy(MergeStrategy.THEIRS)
      .call()

    git_repo.fetch()
      .setRemote(git_remote)
      .call()

    // NOTE: $ git checkout -f master
    git_repo.checkout()
      .setForce(true)
      .setAllPaths(true)
      .setName(default_branch)
      .call()

  } catch {
    case err: Throwable => System.err.println("ERROR pulling: " + err)
  }

  // removing the remote repository reference
  val remote_rm = git_repo.remoteRemove()
  remote_rm.setName(git_remote)
  remote_rm.call()

  // TODO: git_repo.archive()
  //    .setFilename("target/catalogo_ontologie.tar")
  //    .setFormat(null)
  //    .call()

  // call a status just to verify the synchronization was actually finished at this point
  git_repo.status().call()

  // closing git repository
  git_repo.close()

  Thread.sleep(1000)

}