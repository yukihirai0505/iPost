package com.yukihirai0505.iService

import java.io.File

import com.ning.http.client.cookie.Cookie
import com.typesafe.scalalogging.LazyLogging
import com.yukihirai0505.iService.constans.Methods
import com.yukihirai0505.iService.responses.{Status, _}
import com.yukihirai0505.iService.services.UserService.getFollower
import com.yukihirai0505.iService.services.{LikeService, MediaService, UserService}
import com.yukihirai0505.iService.utils.NumberUtil

import scala.concurrent.{ExecutionContext, Future}

class IService(username: String, password: String)
              (implicit ec: ExecutionContext) extends InstagramUser(username, password) with LazyLogging {

  def getUserInfo(targetAccountName: String): Future[Either[Throwable, ProfileUserData]] = {
    def execute(cookies: List[Cookie]) = UserService.getUserInfo(targetAccountName, cookies).flatMap {
      case Right(v) => Future successful Right(v)
      case Left(e) => Future successful Left(e)
    }

    commonAction(execute)
  }

  def getFollowers(targetAccountName: String, queryNum: Int = 50): Future[Either[Throwable, Seq[EdgeFollowedByEdge]]] = {
    def execute(cookies: List[Cookie]) = UserService.getUserInfo(targetAccountName, cookies).flatMap {
      case Right(userData) =>
        logger.info(s"getFollowers userId: ${userData.id} followedBy: ${userData.edgeFollowedBy.count}")
        if (userData.edgeFollowedBy.count > 0) {
          getFollower(userData.id, queryNum, cookies).flatMap {
            case Right(nodes) => Future successful Right(nodes)
            case Left(e) => Future successful Left(e)
          }
        } else Future successful Right(Seq.empty)
      case Left(e) => Future successful Left(e)
    }

    commonAction(execute)
  }

  def getSearchHashTagResult(hashTag: String): Future[Either[Throwable, Tag]] = {
    def execute(cookies: List[Cookie]) = MediaService.getPosts(hashTag, cookies).flatMap {
      case Right(tag) => Future successful Right(tag)
      case Left(e) => Future successful Left(e)
    }

    commonAction(execute)
  }

  def likeMedia(mediaId: String, shortcode: String): Future[Either[Throwable, Status]] = {
    def execute(cookies: List[Cookie]) = LikeService.likeMedia(mediaId, shortcode, cookies)

    commonAction(execute)
  }

  def postNaturalWays(postImage: File, caption: String): Future[Either[Throwable, Status]] = {
    def execute(cookies: List[Cookie]) = MediaService.postNaturalWays(postImage, caption, cookies)

    commonAction(execute)
  }

  def deletePhoto(mediaId: String, shortcode: String): Future[Either[Throwable, Status]] = {
    def execute(cookies: List[Cookie]) = MediaService.deletePhoto(mediaId, shortcode, cookies)

    commonAction(execute)
  }

  private def commonAction[T](execute: (List[Cookie]) => Future[Either[Throwable, T]]) = {
    login().flatMap { cookies: List[Cookie] =>
      Thread.sleep(NumberUtil.getRandomInt())
      execute(cookies)
    }.recover { case e: Exception => Left(e) }
  }
}