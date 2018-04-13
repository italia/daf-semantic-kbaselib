package it.almawave.linkeddata.kb.catalog

import org.eclipse.jgit.util.FileUtils
import com.typesafe.config.Config
import java.nio.file.Paths
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.URIish

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import org.eclipse.jgit.merge.MergeStrategy
import com.typesafe.config.ConfigFactory
import scala.util.Success
import org.slf4j.LoggerFactory

/**
 * REVIEW: trigger of start/stop for git in CatalogBox
 */

object MainGitHandler extends App {

  val conf = ConfigFactory.parseString("""
    git {
    	synchronize: true
    	remote.name: "daf-ontologie"
    	remote.uri: "https://github.com/italia/daf-ontologie-vocabolari-controllati/"
    	local.path: "D://DAF_ontologie-vocabolari-controllati"
    }
  """).getConfig("git")

  val git = GitHandler(conf)
  git.synchronize()

}

object GitHandler {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(conf: Config) = {
    //    val synchronize_repo = conf.getBoolean("synchronize")
    //    logger.debug("synchronization is active? " + synchronize_repo)
    //    if (synchronize_repo)
    //      new GitHandler(conf)
    //    else new GitHandler(ConfigFactory.empty()) {
    //      override def synchronize() = {}
    //    }
    new GitHandler(conf)
  }

}

class GitHandler(conf: Config) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  val remote_name = conf.getString("remote.name")
  val remote_uri = conf.getString("remote.uri")
  val path_local = conf.getString("local.path")
  val git_dir = Paths.get(path_local).normalize()
  val default_branch = "master"

  var git_repo: Git = null

  def start() {

    logger.info(s"using git folder: ${git_dir}")

    git_repo = Git.init().setDirectory(git_dir.toFile()).call()

    // adding the remote repository reference
    val remote_add = git_repo.remoteAdd()
    remote_add.setName(remote_name)
    remote_add.setUri(new URIish(remote_uri))
    remote_add.call()

  }

  def stop() {

    // removing the remote repository reference
    val remote_rm = git_repo.remoteRemove()
    remote_rm.setName(remote_name)
    remote_rm.call()

    // CHECK: call a status just to verify the synchronization was actually finished at this point
    // git_repo.status().call()

    // closing git repository
    git_repo.close()

  }

  def synchronize() {

    logger.info(s"synchronizing git from remote URI: ${remote_uri}")

    start()

    update()

    stop()

  }

  def update() {

    // $ git clean -fd
    git_repo.clean()
      .setForce(true)
      .setCleanDirectories(true)
      .call()

    // download last version
    try {

      // $ git pull daf-ontologie master
      git_repo.pull()
        .setRemote(remote_name)
        .setRemoteBranchName(default_branch)
        .setStrategy(MergeStrategy.THEIRS)
        .call()

      git_repo.fetch()
        .setRemote(remote_name)
        .call()

      // NOTE: $ git checkout -f master
      git_repo.checkout()
        .setForce(true)
        .setAllPaths(true)
        .setName(default_branch)
        .call()

    } catch {
      case err: Throwable => logger.error("ERROR pulling: " + err)
    }

  }

  // TODO: git_repo.archive()
  //    .setFilename("target/catalogo_ontologie.tar")
  //    .setFormat(null)
  //    .call()

  private def delete_git_folder() {
    try {
      FileUtils.delete(git_dir.toFile(), FileUtils.RECURSIVE)
    } catch {
      case err: Throwable => logger.error(s"problems deleting git folder: ${git_dir}")
    }
  }

}
