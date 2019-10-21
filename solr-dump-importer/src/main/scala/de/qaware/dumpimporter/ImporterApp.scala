package de.qaware.dumpimporter

import java.net.URL

import better.files.File
import com.typesafe.scalalogging.Logger
import de.qaware.common.solr.{CloudSolr, LocalSolr, RemoteSolr, SolrRepository, ZKHost}
import de.qaware.dumpimporter.steps.StepContext
import de.qaware.dumpimporter.steps.pide.LoadPIDEMarkupStep
import de.qaware.scalautils.Using
import scopt.{OptionParser, Read}

/** Configuration of the importer.
  *
  * @param dump isabelle dump directory
  * @param solr solr repository to write to
  * @param isValid help flag for argument parsing
  */
sealed case class Config(dump: File = File(""), solr: Option[SolrRepository] = None, isValid: Boolean = true)

/** Command-line interface app for importer. */
object ImporterApp extends App {

  /** Parsing of zookeeper hosts, specified as 'host:port' strings */
  implicit val zkHostRead: Read[ZKHost] = Read.reads(zkhost =>
    zkhost.indexOf(':') match {
      case -1 => throw new IllegalArgumentException("Expected host:port")
      case n: Any => ZKHost(zkhost.take(n), Integer.parseInt(zkhost.drop(n + 1)))
  })

  /** Parse files from better.files */
  implicit val betterFileRead: Read[File] = Read.reads(File(_))

  private val addRepo: (Config, SolrRepository) => Config = (conf, repo) =>
    if (conf.solr.isEmpty) conf.copy(solr = Some(repo)) else conf.copy(isValid = false)

  private val builder = new OptionParser[Config]("solr-dump-importer") {
    head("Imports theory data from isabelle dump into solr")
    arg[File]("<dump>")
      .required()
      .action((file, conf) => conf.copy(dump = file))
      .text("isabelle dump dir")
    opt[File]('l', "local-solr")
      .valueName("<dir>")
      .text("local solr home directory")
      .action((dir, conf) => addRepo(conf, LocalSolr(dir)))
    opt[URL]('e', "external-solr")
      .valueName("<url>")
      .text("external solr host")
      .action((url, conf) => addRepo(conf, RemoteSolr(url)))
    opt[Seq[ZKHost]]('c', "cloud-solr")
      .valueName("<zk1>,<zk2>...")
      .text("solr cloud zookeeper hosts")
      .action((hosts, conf) => addRepo(conf, CloudSolr(hosts)))
    checkConfig(c =>
      if (c.solr.isEmpty) {
        failure("Must specify a solr connection")
      } else if (!c.isValid) {
        failure("Only one solr connection may be specified!")
      } else {
        success
    })
  }

  private val logger = Logger("ImporterApp")

  builder.parse(args, Config()) match {
    case Some(config) =>
      logger.info("Read config: {}", config)
      Using.resource(config.solr.get.solrConnection) { client =>
        logger.info("Connected to solr at {}", config.solr)
        val context = StepContext.empty
        val steps = Seq(new LoadPIDEMarkupStep(config))
        steps.zipWithIndex.foreach({
          case (step, i) =>
            logger.info("Step {}/{}...", i + 1, steps.size)
            step.apply(context)
        })
      }
      logger.info("Finished importing.")
    case _ =>
  }
}
