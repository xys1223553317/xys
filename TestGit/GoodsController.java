package com.jk.xys.controller;

import com.jk.xys.pojo.Comments;
import com.jk.xys.pojo.GoodsBean;
import com.jk.xys.pojo.LogBean;
import com.jk.xys.pojo.NavBean;
import com.jk.xys.service.GoodsService;

import com.jk.xys.utlis.OSSClientUtil;
import com.jk.xys.utlis.uploadOss;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Controller
@RequestMapping("goods")
public class GoodsController {
    @Autowired
    private GoodsService goodsService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private RedisTemplate redisTemplate;
    @RequestMapping("queryNav")
    @ResponseBody
    public List<NavBean> queryNav() {
        return goodsService.queryNav();
    }

    @RequestMapping("queryGoods")
    @ResponseBody
    public HashMap<String, Object> queryGoods(Integer page, Integer rows) {
        return goodsService.queryGoods(page, rows);
    }

    @RequestMapping("addComments")
    @ResponseBody
    public Boolean addComments(Comments comments) {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

            Date date = new Date();
            String format = simpleDateFormat.format(date);
            comments.setCommentDate(format);
            String commentsStars = comments.getCommentsStars();
            String[] split = commentsStars.split(",");
            comments.setCommentsStars(split.length+"");
            mongoTemplate.insert(comments);
            Query query = new Query();
            query.addCriteria(Criteria.where("goodsId").is(comments.getGoodsId()));
            long count = mongoTemplate.count(query,Comments.class);
            goodsService.updateCommentsCount(comments.getGoodsId(),count);
            return true;


        } catch (Exception e) {
            e.printStackTrace();
            return false;


        }
    }
    @RequestMapping("queryComments")
    @ResponseBody
    public  HashMap<String,Object> queryComments(Integer page,Integer rows,Integer id,Comments comments){
        HashMap<String, Object> hashMap = new HashMap<>();
        Query query = new Query();
        if (id!=null) {
            query.addCriteria(Criteria.where("goodsId").is(id));
        }
        if (comments.getComments()!=null&&!comments.getComments().equals("")) {
            Pattern compile = Pattern.compile("^.*"+comments.getComments()+".*$",Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where("comments").regex(comments.getComments()));
        }
        Criteria criteria = new Criteria();
        if(comments.getStartTime()!= null &&! comments.getStartTime().equals("")) {
            criteria = Criteria.where("commentDate").lte(comments.getStartTime());
        }else {
            criteria = Criteria.where("commentDate");
        }
        if(comments.getEndTime() != null&&! comments.getEndTime().equals("") ) {
            criteria.gte(comments.getEndTime());
        }
        if((comments.getStartTime() != null &&! comments.getStartTime().equals(""))|| (comments.getEndTime() != null&&! comments.getEndTime().equals(""))) {
            query.addCriteria(criteria);
        }
        long count = mongoTemplate.count(query,Comments.class);

        //分页
        query.skip((page-1)*rows);
        query.limit(rows);
        List<Comments> comment = mongoTemplate.find(query, Comments.class);

            redisTemplate.opsForValue().set(page, comment);
        List<Comments> ob = (List<Comments>) redisTemplate.opsForValue().get(page);

        if (ob != null && !ob.toString().equals("")&&ob.size()>0) {
            hashMap.put("rows", ob);
            hashMap.put("total", count);
            return hashMap;
        }

        hashMap.put("total", count);
        hashMap.put("rows", comment);
        return hashMap;
    }
    @RequestMapping("queryLog")
    @ResponseBody
    public  HashMap<String,Object> queryLog(Integer page,Integer rows){
        HashMap<String, Object> hashMap = new HashMap<>();
        Query query = new Query();

        long count = mongoTemplate.count(query, LogBean.class);


        query.skip((page-1)*rows);
        query.limit(rows);
        List<LogBean> comments = mongoTemplate.find(query, LogBean.class);

        hashMap.put("total", count);
        hashMap.put("rows", comments);
        return hashMap;
    }
    @RequestMapping(value = "upload", method = RequestMethod.POST)
    @ResponseBody
    public Map<String,Object> headImg(HttpServletRequest request , MultipartFile file){
        Map<String, Object> value = new HashMap<String, Object>();
        value.put("success", true);
        value.put("errorCode", 0);
        value.put("errorMsg", "");
        String head = goodsService.updateHead(file, 4);//此处是调用上传服务接口，4是需要更新的userId 测试数据。
        System.out.println(head);
        value.put("data", head);
        /**
         * 赋值进去
         */
        //files.setFileUrl(head);
        //  orderServiceFegin.addFile(files);
        return value;
    }




    @RequestMapping("addGoods")
    @ResponseBody
    public Boolean addGoods(GoodsBean goodsBean){
        try {
            goodsService.addGoods(goodsBean);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
    @RequestMapping("downImg")
    @ResponseBody
    public String testDownload(String img) {
        int i = img.indexOf("h");
        int end=img.indexOf("?");
        String substring1 = img.substring(i, end);


        String substring = substring1.substring(43);
        System.out.println(substring);
        String name = substring.substring(5);
        System.out.println(name);
        uploadOss aliyunOSSUtil = new uploadOss();
        aliyunOSSUtil.downloadFile(substring,"f:/img/"+name);
        return "success";
    }
}