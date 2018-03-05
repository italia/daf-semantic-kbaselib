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

object GitHandler {

  def apply(conf: Config) = {
    val synchronize_repo = conf.getBoolean("synchronize")
    if (synchronize_repo)
      new GitHandler(conf)
    else new GitHandler(ConfigFactory.empty()) {
      override def synchronize() = Future{}
    }
  }

}

class GitHandler(conf: Config) {

  val remote_name = conf.getString("remote.name")
  val remote_uri = conf.getString("remote.uri")

  val git_root = conf.getString("local.path")
  val git_dir = Paths.get(git_root, ".git").normalize()

  val default_branch = "master"
  val synchronize_repo = conf.getBoolean("synchronize")

  private var git_repo: Git = null

  def start() {

    if (git_repo == null) {
      git_repo = Git.init().setDirectory(git_dir.toFile()).call()

      // adding the remote repository reference
      val remote_add = git_repo.remoteAdd()
      remote_add.setName(remote_name)
      remote_add.setUri(new URIish(remote_uri))
      remote_add.call()

    }
  }

  def stop() {

    // removing the remote repository reference
    val remote_rm = git_repo.remoteRemove()
    remote_rm.setName(remote_name)
    remote_rm.call()

    Thread.sleep(1000)

    if (git_repo != null)
      git_repo.close()

  }

  def synchronize() = Future {
    this.start()
    this.update()
    this.delete_git_folder()
    this.stop()
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

      // $ git fetch daf-ontologie
      git_repo.fetch()
        .setRemote(remote_name)
        .call()

      // $ git checkout -f master
      git_repo.checkout()
        .setForce(true)
        .setAllPaths(true)
        .setName(default_branch)
        .call()

    } catch {
      case err: Throwable => System.err.println("ERROR pulling: " + err)
    }

    // CHECK: the idea is to save elsewhere the latest working copy of ontologies / vocabularies
    // git_repo.archive()
    // .setFilename("target/catalogo_ontologie.tar")
    // .setFormat(null)
    // .call()

    // call a status just to verify the synchronization was actually finished at this point
    git_repo.status().call()

  }

  private def delete_git_folder() {
    try {
      FileUtils.delete(git_dir.toFile(), FileUtils.RECURSIVE)
    } catch {
      case err: Throwable => System.err.println(s"problems deleting git folder: ${git_dir}")
    }
  }

}


