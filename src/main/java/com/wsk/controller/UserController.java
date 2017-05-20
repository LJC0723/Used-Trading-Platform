package com.wsk.controller;

import com.wsk.pojo.*;
import com.wsk.service.*;
import com.wsk.token.TokenProccessor;
import com.wsk.tool.StringUtils;
import com.wsk.tool.empty.Empty;
import com.wsk.tool.encrypt.Encrypt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;

/**
 * Created by wsk1103 on 2017/5/9.
 */
@Controller
public class UserController {

    @Resource
    private UserInformationService userInformationService;
    @Resource
    private UserPasswordService userPasswordService;
    @Resource
    private UserCollectionService userCollectionService;
    @Resource
    private UserReleaseService userReleaseService;
    @Resource
    private BoughtShopService boughtShopService;
    @Resource
    private UserWantService userWantService;
    @Resource
    private ShopCarService shopCarService;
    @Resource
    private OrderFormService orderFormService;
    @Resource
    private GoodsOfOrderFormService goodsOfOrderFormService;
    @Resource
    private UserStateService userStateService;
    @Resource
    private ShopInformationService shopInformationService;
    @Resource
    private GoodsCarService goodsCarService;
    @Resource
    private SpecificeService specificeService;
    @Resource
    private ClassificationService classificationService;
    @Resource
    private AllKindsService allKindsService;

//    @RequestMapping("/")
//    public String wsk(Model model) {
//        model.addAttribute("wsk", "wsk");
//        return "/index";
//    }

    //进入登录界面
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String login(HttpServletRequest request, Model model) {
        String loginToken = TokenProccessor.getInstance().makeToken();
        String registerToken = TokenProccessor.getInstance().makeToken();
        String forgetToken = TokenProccessor.getInstance().makeToken();
        request.getSession().setAttribute("loginToken", loginToken);
        request.getSession().setAttribute("registerToken", registerToken);
        request.getSession().setAttribute("forgetToken", forgetToken);
        model.addAttribute("registerToken", registerToken);
        model.addAttribute("loginToken", loginToken);
        model.addAttribute("forgetToken", forgetToken);
        return "page/login_page";
    }

    //用户注册,拥有插入数据而已，没什么用的
    @RequestMapping(value = "/registered", method = RequestMethod.POST)
    public String registered(Model model,
                             @RequestParam String name, @RequestParam String phone, @RequestParam String password) {
        UserInformation userInformation = new UserInformation();
        userInformation.setUsername(name);
        userInformation.setPhone(phone);
        userInformation.setModified(new Date());
        userInformation.setCreatetime(new Date());
        if (userInformationService.insertSelective(userInformation) == 1) {
            int uid = userInformationService.selectIdByPhone(phone);
            UserPassword userPassword = new UserPassword();
            userPassword.setModified(new Date());
            password = Encrypt.getMD5(password);
            userPassword.setPassword(password);
            userPassword.setUid(uid);
            int result = userPasswordService.insertSelective(userPassword);
            if (result != 1) {
                model.addAttribute("result", "fail");
                return "success";
            }
            model.addAttribute("result", "success");
            return "success";
        }
        model.addAttribute("result", "fail");
        return "success";
    }

    //用户注册
//    @RequestMapping(value = "/registered", method = RequestMethod.GET)
//    public String registered() {
//        return "registered";
//    }

    //验证登录
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    @ResponseBody
    public Map login(HttpServletRequest request, Model model,
                     @RequestParam String phone, @RequestParam String password, @RequestParam String token) {
        String loginToken = (String) request.getSession().getAttribute("loginToken");
        Map<String, Integer> map = new HashMap<>();
        if (Empty.isNullOrEmpty(phone) || Empty.isNullOrEmpty(password)) {
            map.put("wsk", 2);
            return map;
        }
        //防止重复提交
        if (Empty.isNullOrEmpty(loginToken) || !token.equals(loginToken)) {
            map.put("wsk", 1);
            return map;
        } else {
            request.getSession().removeAttribute("loginToken");
        }
        boolean b = getId(phone, password, request);
        //失败，不存在该手机号码
        if (!b) {
//            model.addAttribute("error", "手机号码或者密码错误");
//            model.addAttribute("phone", phone);
            map.put("wsk", 2);
            return map;
        }
        map.put("wsk", 3);
        return map;
    }

    //查看用户基本信息
    @RequestMapping(value = "/personal_info")
    public String personalInfo(HttpServletRequest request, Model model) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (Empty.isNullOrEmpty(userInformation)) {
            return "";
        }
        String personalInfoToken = TokenProccessor.getInstance().makeToken();
        request.getSession().setAttribute("personalInfoToken", personalInfoToken);
        model.addAttribute("token", personalInfoToken);
        model.addAttribute("userInformation", userInformation);
        return "page/personal/personal_info";
    }


    //完善用户基本信息，认证
    @RequestMapping(value = "/certification", method = RequestMethod.POST)
    public String certification(HttpServletRequest request, Model model,
                                @RequestParam(required = false) String realName,
                                @RequestParam(required = false) String clazz, @RequestParam String token,
                                @RequestParam(required = false) String sno, @RequestParam(required = false) String dormitory,
                                @RequestParam(required = false) String gender) {
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        //该用户还没有登录
        if (Empty.isNullOrEmpty(userInformation)) {
            return "";
        }
        String certificationToken = (String) request.getSession().getAttribute("personalInfoToken");
        //防止重复提交
        if (Empty.isNullOrEmpty(certificationToken) || !certificationToken.equals(token)) {
            return "";
        }
//        else {
//            request.getSession().removeAttribute("certificationToken");
//        }
        if (realName != null)
            realName = StringUtils.replaceBlank(realName);
        if (clazz != null)
            clazz = StringUtils.replaceBlank(clazz);
        if (sno != null)
            sno = StringUtils.replaceBlank(sno);
        if (dormitory != null)
            dormitory = StringUtils.replaceBlank(dormitory);
        if (gender != null)
            gender = StringUtils.replaceBlank(gender);
        //数据格式错误
        if (realName.length() > 25 || clazz.length() > 25 || sno.length() > 12 || dormitory.length() > 25 || gender.length() > 2) {
            return "";
        }
        if (realName.length() < 1 || clazz.length() < 1 || sno.length() < 1 || dormitory.length() < 1 || gender.length() < 1) {
            return "";
        }
        userInformation.setRealname(realName);
        userInformation.setClazz(clazz);
        userInformation.setModified(new Date());
        userInformation.setSno(sno);
        userInformation.setDormitory(dormitory);
        userInformation.setGender(gender);
        int result = userInformationService.updateByPrimaryKeySelective(userInformation);
        if (result != 1) {
            //更新失败，认证失败
            return "";
        }
        //认证成功
        return "";
    }

    //enter the publishUserWant.html,进入求购页面
    @RequestMapping(value = "enterPublishUserWant")
    public String enterPublishUserWant(HttpServletRequest request, Model model) {
        if (Empty.isNullOrEmpty(request.getSession().getAttribute("userInformation"))) {
            return "";
        }
        String publishUserWantToken = TokenProccessor.getInstance().makeToken();
        request.getSession().setAttribute("publishUserWantToken", publishUserWantToken);
        model.addAttribute("token", publishUserWantToken);
        return "";
    }

    //publish userWant,发布求购
    @RequestMapping(value = "/publishUserWant")
    @ResponseBody
    public Map publishUserWant(HttpServletRequest request, @RequestParam String name,
                               @RequestParam int sort, @RequestParam int quantity,
                               @RequestParam double price, @RequestParam String remark,
                               @RequestParam String token) {
        Map<String, Integer> map = new HashMap<>();
        //determine whether the user exits
        if (Empty.isNullOrEmpty(request.getSession().getAttribute("userInformation"))) {
            //if the user no exits in the session,
            map.put("result", 2);
            return map;
        }
        String publishUserWantToke = (String) request.getSession().getAttribute("publishUserWantToken");
        if (Empty.isNullOrEmpty(publishUserWantToke) || !publishUserWantToke.equals(token)) {
            map.put("result", 2);
            return map;
        } else {
            request.getSession().removeAttribute("publishUserWantToken");
        }
        UserWant userWant = new UserWant();
        userWant.setModified(new Date());
        userWant.setName(name);
        userWant.setPrice(new BigDecimal(price));
        userWant.setQuantity(quantity);
        userWant.setRemark(remark);
        userWant.setUid((Integer) request.getSession().getAttribute("uid"));
        userWant.setSort(sort);
        int result = 0;
        try {
            result = userWantService.insertSelective(userWant);
            if (result != 1) {
                map.put("result", result);
                return map;
            }
        } catch (Exception e) {
            e.printStackTrace();
            map.put("result", result);
            return map;
        }
        map.put("result", result);
        return map;
    }

    //getUserWant,查看求购
    @RequestMapping(value = "/getUserWant")
    @ResponseBody
    public List getUserWant(HttpServletRequest request, Model model,
                            @RequestParam int start) {
        List<UserWant> list = new ArrayList<>();
        if (Empty.isNullOrEmpty(request.getSession().getAttribute("userInformation"))) {
            UserWant userWant = new UserWant();
            list.add(userWant);
            return list;
        }
        try {
            int uid = (int) request.getSession().getAttribute("uid");
            list = selectUserWantByUid(uid, start);
        } catch (Exception e) {
            e.printStackTrace();
            return list;
        }
        model.addAttribute("userWant", list);
        return list;
    }

    //getUserWantCounts,查看求购总数
    @RequestMapping(value = "/getUserWantCounts")
    @ResponseBody
    public Map getUserWantCounts(HttpServletRequest request, Model model) {
        Map<String, Integer> map = new HashMap<>();
        if (Empty.isNullOrEmpty(request.getSession().getAttribute("userInformation"))) {
            map.put("counts", -1);
            return map;
        }
        try {
            int counts = getUserWantCounts((Integer) request.getSession().getAttribute("uid"));
            map.put("counts", counts);
        } catch (Exception e) {
            e.printStackTrace();
            map.put("counts", -1);
        }
        return map;
    }

    //删除求购
    @RequestMapping(value = "/deleteUserWant")
    @ResponseBody
    public Map deleteUserWant(HttpServletRequest request, @RequestParam int uwid) {
        Map<String, Integer> map = new HashMap<>();
        if (Empty.isNullOrEmpty(request.getSession().getAttribute("userInformation"))) {
            map.put("result", 2);
            return map;
        }
        UserWant userWant = new UserWant();
        userWant.setId(uwid);
        userWant.setDisplay(0);
        try {
            int result = userWantService.updateByPrimaryKeySelective(userWant);
            if (result != 1) {
                map.put("result", 0);
                return map;
            }
        } catch (Exception e) {
            e.printStackTrace();
            map.put("result", 0);
        }
        map.put("result", 1);
        return map;
    }

    //收藏
    //add the userCollection
    @RequestMapping(value = "/addUserCollection")
    @ResponseBody
    public Map addUserCollection(HttpServletRequest request, @RequestParam int sid) {
        Map<String, Integer> map = new HashMap<>();
        //determine whether the user exits
        if (Empty.isNullOrEmpty(request.getSession().getAttribute("userInformation"))) {
            //if the user no exits in the session,
            map.put("result", 0);
            return map;
        }
        UserCollection userCollection = new UserCollection();
        userCollection.setModified(new Date());
        userCollection.setSid(sid);
        userCollection.setUid((Integer) request.getSession().getAttribute("uid"));
        int result = 0;
        try {
            //begin insert the userCollection
            result = userCollectionService.insertSelective(userCollection);
        } catch (Exception e) {
            e.printStackTrace();
            map.put("result", result);
            return map;
        }
        if (result != 1) {
            map.put("result", result);
            return map;
        }
        map.put("result", result);
        return map;
    }


    // delete the userCollection
    @RequestMapping(value = "/deleteUserCollection")
    @ResponseBody
    public Map deleteUserCollection(HttpServletRequest request, @RequestParam int ucid) {
        Map<String, Integer> map = new HashMap<>();
        if (Empty.isNullOrEmpty(request.getSession().getAttribute("userInformation"))) {
            map.put("result", 0);
            return map;
        }
        UserCollection userCollection = new UserCollection();
//        userCollection.setUid((Integer) request.getSession().getAttribute("uid"));
//        userCollection.setSid(sid);
        userCollection.setId(ucid);
        userCollection.setModified(new Date());
        userCollection.setDisplay(0);
        int result = 0;
        try {
            result = userCollectionService.updateByPrimaryKeySelective(userCollection);
            if (result != 1) {
                map.put("result", result);
                return map;
            }
        } catch (Exception e) {
            e.printStackTrace();
            map.put("result", result);
            return map;
        }
        map.put("result", result);
        return map;
    }

    //购物车开始。。。。。。。。。。。
    //getShopCarCounts
    @RequestMapping(value = "/getShopCarCounts")
    @ResponseBody
    public Map getShopCarCounts(HttpServletRequest request) {
        Map<String, Integer> map = new HashMap<>();
        if (Empty.isNullOrEmpty(request.getSession().getAttribute("userInformation"))) {
            map.put("counts", -1);
            return map;
        }
        int uid = (int) request.getSession().getAttribute("uid");
        int counts = getShopCarCounts(uid);
        map.put("counts", counts);
        return map;
    }

    //check the shopping cart,查看购物车
    @RequestMapping(value = "/selectShopCar")
    @ResponseBody
    public ShopCar selectShopCar(HttpServletRequest request) {
//        List<ShopCar> list = new ArrayList<>();
        if (Empty.isNullOrEmpty(request.getSession().getAttribute("userInformation"))) {
            ShopCar shopCar = new ShopCar();
//            list.add(shopCar);
            return shopCar;
        }
        int uid = (int) request.getSession().getAttribute("uid");
        return selectShopCarByUid(uid);
    }

    //通过购物车的id获取购物车里面的商品
    @RequestMapping(value = "/selectGoodsOfShopCar")
    @ResponseBody
    public List<GoodsCar> selectGoodsCar(HttpServletRequest request) {
        List<GoodsCar> list = new ArrayList<>();
        GoodsCar goodsCar = new GoodsCar();
        if (Empty.isNullOrEmpty(request.getSession().getAttribute("userInformation"))) {
            list.add(goodsCar);
            return list;
        }
        try {
            int scid = shopCarService.selectByUid((Integer) request.getSession().getAttribute("uid")).getId();
            list = goodsCarService.selectBySCid(scid);
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return list;
        }
    }

    //添加到购物车
    @RequestMapping(value = "/insertGoodsCar")
    @ResponseBody
    public Map insertGoodsCar(HttpServletRequest request, @RequestParam int id) {
        Map<String, Integer> map = new HashMap<>();
        if (Empty.isNullOrEmpty(request.getSession().getAttribute("userInformation"))) {
            map.put("result", 2);
            return map;
        }
        try {
            int uid = (int) request.getSession().getAttribute("uid");
            //获取购物车
            ShopCar shopCar = shopCarService.selectByUid(uid);
            if (Empty.isNullOrEmpty(shopCar)) {
                shopCar.setModified(new Date());
                shopCar.setDisplay(1);
                shopCar.setUid(uid);
                try {
                    shopCarService.insertSelective(shopCar);
                    shopCar = shopCarService.selectByUid(uid);
                } catch (Exception e) {
                    e.printStackTrace();
                    map.put("result", 0);
                    return map;
                }
            }
            GoodsCar goodsCar = new GoodsCar();
            goodsCar.setDisplay(1);
            goodsCar.setModified(new Date());
            goodsCar.setQuantity(1);
            goodsCar.setScid(shopCar.getId());
            goodsCar.setSid(id);
            try {
                int result = goodsCarService.insertSelective(goodsCar);
                map.put("result", result);
                return map;
            } catch (Exception e) {
                e.printStackTrace();
                map.put("result", 0);
                return map;
            }
        } catch (Exception e) {
            e.printStackTrace();
            map.put("result", 0);
            return map;
        }
    }

    //删除购物车的商品
    @RequestMapping(value = "/deleteShopCar")
    @ResponseBody
    public Map deleteShopCar(HttpServletRequest request, @RequestParam int sid) {
        Map<String, Integer> map = new HashMap<>();
        if (Empty.isNullOrEmpty(request.getSession().getAttribute("userInformation"))) {
            map.put("result", 2);
            return map;
        }
//        ShopCar shopCar = new ShopCar();
//        shopCar.setDisplay(0);
//        shopCar.setId(gcid);
//        shopCar.setModified(new Date());
        //获得用户购物车的id
        int scid = shopCarService.selectByUid((Integer) request.getSession().getAttribute("uid")).getId();
        GoodsCar goodsCar = new GoodsCar();
        goodsCar.setDisplay(0);
        goodsCar.setSid(sid);
        goodsCar.setScid(scid);
        try {
            int result = goodsCarService.updateByPrimaryKey(goodsCar);
            if (result != 1) {
                map.put("result", result);
                return map;
            }
        } catch (Exception e) {
            e.printStackTrace();
            map.put("result", 0);
            return map;
        }
        map.put("result", 1);
        return map;
    }

    //发布商品
    @RequestMapping(value = "/insertGoods", method = RequestMethod.POST)
    public String insertGoods(@RequestParam String name, @RequestParam int level,
                              @RequestParam String remark, @RequestParam double price,
                              @RequestParam int sort, @RequestParam int quantity,
                              @RequestParam String token, @RequestParam(required = false) MultipartFile image,
                              @RequestParam int action,@RequestParam(required = false) int id,
                              HttpServletRequest request, Model model) {
        String goodsToken = (String) request.getSession().getAttribute("goodsToken");
//        String publishProductToken = TokenProccessor.getInstance().makeToken();
//        request.getSession().setAttribute("token",publishProductToken);
        //防止重复提交
        if (Empty.isNullOrEmpty(goodsToken) || !goodsToken.equals(token)) {
            return "/publish_product";
        } else {
            request.getSession().removeAttribute("goodsToken");
        }
//        //从session中获得用户的基本信息
        UserInformation userInformation = (UserInformation) request.getSession().getAttribute("userInformation");
        if (Empty.isNullOrEmpty(userInformation)) {
            //如果用户不存在，
            return "";
        }
        //插入
        if (action==1) {
            if (Empty.isNullOrEmpty(image)) {
                model.addAttribute("message", "请选择图片!!!");
                return "/publish_product";
            }
            String random;
            String path = "D:\\";
            random = "image\\" + StringUtils.getRandomChar() + new Date().getTime() + ".jpg";
//        String fileName = "\\" + random + ".jpg";
            File file = new File(path, random);
            if (!file.exists()) {
                file.mkdir();
            }
            try {
                image.transferTo(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
            name = StringUtils.replaceBlank(name);
            remark = StringUtils.replaceBlank(remark);
            //judge the data`s format
            if (Empty.isNullOrEmpty(name) || Empty.isNullOrEmpty(level) || Empty.isNullOrEmpty(remark) || Empty.isNullOrEmpty(price)
                    || Empty.isNullOrEmpty(sort) || Empty.isNullOrEmpty(quantity) || name.length() > 25 || remark.length() > 122) {
                model.addAttribute("message", "请输入正确的格式!!!!!");
                model.addAttribute("token", goodsToken);
                return "/publish_product";
            }
            //begin insert the shopInformation to the MySQL
            ShopInformation shopInformation = new ShopInformation();
            shopInformation.setName(name);
            shopInformation.setLevel(level);
            shopInformation.setRemark(remark);
            shopInformation.setPrice(new BigDecimal(price));
            shopInformation.setSort(sort);
            shopInformation.setQuantity(quantity);
            shopInformation.setModified(new Date());
            shopInformation.setImage(random);//This is the other uniquely identifies
//        shopInformation.setUid(4);
            int uid = (int) request.getSession().getAttribute("uid");
            shopInformation.setUid(uid);
            try {
                int result = shopInformationService.insertSelective(shopInformation);
                //插入失败？？？
                if (result != 1) {
                    model.addAttribute("message", "请输入正确的格式!!!!!");
                    model.addAttribute("token", goodsToken);
                    return "/publish_product";
                }
            } catch (Exception e) {
                e.printStackTrace();
                model.addAttribute("token", goodsToken);
                model.addAttribute("message", "请输入正确的格式!!!!!");
                return "/publish_product";
            }
            int sid = shopInformationService.selectIdByImage(random);// get the id which is belongs shopInformation
            //将发布的商品的编号插入到用户的发布中
            UserRelease userRelease = new UserRelease();
            userRelease.setModified(new Date());
            userRelease.setSid(sid);
            userRelease.setUid(uid);
            try {
                int result = userReleaseService.insertSelective(userRelease);
                //如果关联失败，删除对应的商品和商品图片
                if (result != 1) {
                    //if insert failure,transaction rollback.
                    shopInformationService.deleteByPrimaryKey(sid);
//                shopPictureService.deleteByPrimaryKey(spid);
                    model.addAttribute("token", goodsToken);
                    model.addAttribute("message", "请输入正确的格式!!!!!");
                    return "/publish_product";
                }
            } catch (Exception e) {
                //if insert failure,transaction rollback.
                shopInformationService.deleteByPrimaryKey(sid);
//            shopPictureService.deleteByPrimaryKey(spid);
                e.printStackTrace();
                model.addAttribute("token", goodsToken);
                model.addAttribute("message", "请输入正确的格式!!!!!");
                return "/publish_product";
            }

            //publish success
//        model.addAttribute("shopPicture", shopPicture);
            shopInformation.setId(sid);
            goodsToken = TokenProccessor.getInstance().makeToken();
            request.getSession().setAttribute("goodsToken", goodsToken);
            model.addAttribute("token", goodsToken);
            model.addAttribute("shopInformation", shopInformation);
            model.addAttribute("userInformation", userInformation);
            String sb = getSort(sort);
            model.addAttribute("sort", sb);
            model.addAttribute("action", 2);
        } else if (action==2){
            ShopInformation shopInformation = new ShopInformation();
            shopInformation.setModified(new Date());
            shopInformation.setQuantity(quantity);
            shopInformation.setSort(sort);
            shopInformation.setPrice(new BigDecimal(price));
            shopInformation.setRemark(remark);
            shopInformation.setLevel(level);
            shopInformation.setName(name);
            shopInformation.setId(id);
            try {
                int result = shopInformationService.updateByPrimaryKeySelective(shopInformation);
                if (result != 1) {
                    return "/publish_product";
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "/publish_product";
            }
            goodsToken = TokenProccessor.getInstance().makeToken();
            request.getSession().setAttribute("goodsToken", goodsToken);
            model.addAttribute("token", goodsToken);
            shopInformation = shopInformationService.selectByPrimaryKey(id);
            model.addAttribute("userInformation", userInformation);
            model.addAttribute("shopInformation", shopInformation);
            model.addAttribute("action", 2);
            model.addAttribute("sort", getSort(sort));
        }
        return "page/publish_product";
    }

    private String getSort(int sort) {
        StringBuilder sb = new StringBuilder();
        Specific specific = selectSpecificBySort(sort);
        int cid = specific.getCid();
        Classification classification = selectClassificationByCid(cid);
        int aid = classification.getAid();
        AllKinds allKinds = selectAllKindsByAid(aid);
        sb.append(allKinds.getName());
        sb.append("-");
        sb.append(classification.getName());
        sb.append("-");
        sb.append(specific.getName());
        return sb.toString();
    }

    //下架商品
    @RequestMapping(value = "/deleteShop")
    @ResponseBody
    public Map deleteShop(HttpServletRequest request, @RequestParam int sid) {
        Map<String, Integer> map = new HashMap<>();
        if (Empty.isNullOrEmpty(request.getSession().getAttribute("userInformation"))) {
            map.put("result", 2);
            return map;
        }
        ShopInformation shopInformation = new ShopInformation();
        shopInformation.setModified(new Date());
        shopInformation.setDisplay(0);
        shopInformation.setId(sid);
        try {
            int result = shopInformationService.updateByPrimaryKeySelective(shopInformation);
            if (result != 1) {
                map.put("result", 0);
                return map;
            }
            map.put("result", result);
        } catch (Exception e) {
            e.printStackTrace();
            map.put("result", 0);
        }
        return map;
    }

    //查看发布的所有商品总数
    @RequestMapping(value = "/getReleaseShopCounts")
    @ResponseBody
    public Map getReleaseShopCounts(HttpServletRequest request) {
        Map<String, Integer> map = new HashMap<>();
        if (Empty.isNullOrEmpty(request.getSession().getAttribute("userInformation"))) {
            map.put("counts", -1);
            return map;
        }
        int counts = getReleaseCounts((Integer) request.getSession().getAttribute("uid"));
        map.put("counts", counts);
        return map;
    }

    //查看我的发布的商品
    @RequestMapping(value = "/getReleaseShop")
    @ResponseBody
    public List getReleaseShop(HttpServletRequest request, @RequestParam int start) {
        List<UserRelease> list = new ArrayList<>();
        if (Empty.isNullOrEmpty(request.getSession().getAttribute("userInformation"))) {
            UserRelease userRelease = new UserRelease();
            list.add(userRelease);
            return list;
        }
        list = selectReleaseByUid((Integer) request.getSession().getAttribute("uid"), start);
        return list;
    }

    //更新商品信息


    //查看用户收藏的货物的总数
    private int getCollectionCounts(int uid) {
        int counts;
        try {
            counts = userCollectionService.getCounts(uid);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return counts;
    }

    //查看收藏，一次10个
    private List<UserCollection> selectContectionByUid(int uid, int start) {
        try {
            return userCollectionService.selectByUid(uid, (start - 1) * 10);
        } catch (Exception e) {
            e.printStackTrace();
            List<UserCollection> list = new ArrayList<>();
            list.add(new UserCollection());
            return list;
        }
    }

    //查看用户发布的货物的总数
    private int getReleaseCounts(int uid) {
        try {
            return userReleaseService.getCounts(uid);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    //查看发布的货物，一次10个
    private List<UserRelease> selectReleaseByUid(int uid, int start) {
        try {
            return userReleaseService.selectByUid(uid, (start - 1) * 10);
        } catch (Exception e) {
            e.printStackTrace();
            List<UserRelease> list = new ArrayList<>();
            list.add(new UserRelease());
            return list;
        }
    }

    //查看用户购买到的物品的总数
    private int getBoughtShopCounts(int uid) {
        try {
            return boughtShopService.getCounts(uid);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    //查看用户的购买，10个
    private List<BoughtShop> selectBoughtShopByUid(int uid, int start) {
        try {
            return boughtShopService.selectByUid(uid, (start - 1) * 10);
        } catch (Exception e) {
            e.printStackTrace();
            List<BoughtShop> list = new ArrayList<>();
            list.add(new BoughtShop());
            return list;
        }
    }

    //查看用户的求购总个数
    private int getUserWantCounts(int uid) {
        try {
            return userWantService.getCounts(uid);
        } catch (Exception e) {
            return -1;
        }
    }

    //求购列表10
    private List<UserWant> selectUserWantByUid(int uid, int start) {
        try {
            return userWantService.selectByUid(uid, (start - 1) * 10);
        } catch (Exception e) {
            e.printStackTrace();
            List<UserWant> list = new ArrayList<>();
            list.add(new UserWant());
            return list;
        }
    }

    //我的购物车总数
    private int getShopCarCounts(int uid) {
        try {
            return shopCarService.getCounts(uid);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    //购物车列表  10
    private ShopCar selectShopCarByUid(int uid) {
        try {
            return shopCarService.selectByUid(uid);
        } catch (Exception e) {
            e.printStackTrace();
//            List<ShopCar> list
            return new ShopCar();
        }
    }

    //查看订单总数
    private int getOrderFormCounts(int uid) {
        try {
            return orderFormService.getCounts(uid);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    //订单列表 10个
    private List<OrderForm> selectOrderFormByUid(int uid, int start) {
        try {
            return orderFormService.selectByUid(uid, (start - 1) * 10);
        } catch (Exception e) {
            e.printStackTrace();
            List<OrderForm> list = new ArrayList<>();
            list.add(new OrderForm());
            return list;
        }
    }

    //订单中的商品
    private List<GoodsOfOrderForm> selectGoodsOfOrderFormByOFid(int ofid) {
        try {
            return goodsOfOrderFormService.selectByOFid(ofid);
        } catch (Exception e) {
            e.printStackTrace();
            List<GoodsOfOrderForm> list = new ArrayList<>();
            list.add(new GoodsOfOrderForm());
            return list;
        }
    }

    //查看用户的状态
    private UserState selectUserStateByUid(int uid) {
        try {
            return userStateService.selectByUid(uid);
        } catch (Exception e) {
            e.printStackTrace();
            return new UserState();
        }
    }

    //判断该手机号码及其密码是否一一对应
    private boolean getId(String phone, String password, HttpServletRequest request) {
        int uid = userInformationService.selectIdByPhone(phone);
        if (Empty.isNullOrEmpty(uid)) {
            return false;
        }
        UserInformation userInformation = userInformationService.selectByPrimaryKey(uid);
        password = Encrypt.getMD5(password);
        String password2 = userPasswordService.selectByUid(userInformation.getId()).getPassword();
        if (!password.equals(password2)) {
            return false;
        }
        //如果密码账号对应正确，将userInformation存储到session中
        request.getSession().setAttribute("userInformation", userInformation);
        request.getSession().setAttribute("uid", uid);
        return true;
    }

    //获取最详细的分类，第三层
    private Specific selectSpecificBySort(int sort) {
        return specificeService.selectByPrimaryKey(sort);
    }

    //获得第二层分类
    private Classification selectClassificationByCid(int cid) {
        return classificationService.selectByPrimaryKey(cid);
    }

    //获得第一层分类
    private AllKinds selectAllKindsByAid(int aid) {
        return allKindsService.selectByPrimaryKey(aid);
    }
}