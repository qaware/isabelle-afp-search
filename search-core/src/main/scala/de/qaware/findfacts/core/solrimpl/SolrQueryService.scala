package de.qaware.findfacts.core.solrimpl

import scala.collection.JavaConverters._
import scala.language.{postfixOps, reflectiveCalls}
import scala.util.Try

import com.typesafe.scalalogging.Logger
import de.qaware.findfacts.common.dt.BaseEt
import de.qaware.findfacts.common.solr.SolrSchema
import de.qaware.findfacts.common.solr.mapper.FromSolrDoc
import de.qaware.findfacts.common.utils.TryUtils.flattenTryFailFirst
import de.qaware.findfacts.core.{FacetQuery, FilterQuery, QueryService, ShortEntry}
import org.apache.solr.client.solrj
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.response.QueryResponse

/** Solr impl of the query service.
  *
  * @param connection solr instance
  * @param mapper solr query mapper
  */
class SolrQueryService(connection: SolrClient, mapper: SolrQueryMapper) extends QueryService {
  private val logger = Logger[SolrQueryService]

  /** Get result from solr */
  private def getSolrResult(query: solrj.SolrQuery): Try[QueryResponse] = {
    Try {
      logger.info(s"Executing query $query")
      val resp = connection.query(query)
      if (resp.getStatus != 0 && resp.getStatus != 200) {
        throw new IllegalStateException(s"Query status was not ok: $resp")
      }
      resp
    }
  }

  override def getFacetResults(facetQuery: FacetQuery): Try[Map[String, Long]] = {
    for {
      solrQuery <- mapper.buildQuery(this, facetQuery)
      solrResult <- getSolrResult(solrQuery)
      result <- Try {
        solrResult
          .getFacetField(SolrSchema.getFieldName(facetQuery.field))
          .getValues
          .asScala
          .groupBy(_.getName)
          .mapValues(_.head.getCount)
      }
    } yield result
  }

  /** Get result docs and map to entity. */
  private def getListResults[A](filterQuery: FilterQuery)(implicit docMapper: FromSolrDoc[A]): Try[Vector[A]] = {
    val results = for {
      query <- mapper.buildQuery(this, filterQuery)
      solrRes <- getSolrResult(query)
    } yield solrRes.getResults.asScala.toSeq.map(docMapper.fromSolrDoc)

    (results: Try[Seq[A]]).map(_.toVector)
  }

  override def getResults(filterQuery: FilterQuery): Try[Vector[BaseEt]] = {
    getListResults(filterQuery)(FromSolrDoc[BaseEt])
  }

  override def getShortResults(filterQuery: FilterQuery): Try[Vector[ShortEntry]] = {
    getListResults(filterQuery)(FromSolrDoc[ShortEntry])
  }
}
