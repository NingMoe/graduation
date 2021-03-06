package com.gayson.controller;

import com.gayson.exception.ApplicationException;
import com.gayson.exception.ErrorCode;
import com.gayson.globals.Result;
import com.gayson.globals.ResultStatus;
import com.gayson.model.Restaurant;
import com.gayson.model.User;
import com.gayson.repos.RestaurantRepository;
import com.gayson.repos.UserRepository;
import com.gayson.security.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

/**
 * Created by jixunzhen on 2018/5/14.
 */
@RestController
@PreAuthorize("hasRole('USER')")
@RequestMapping(path = "/restaurant")
public class RestaurantController {
    @Autowired
    HttpServletRequest request;

    @Autowired
    JwtTokenUtil jwtTokenUtil;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RestaurantRepository restaurantRepository;

    @RequestMapping(path = "/register_restaurant", method = RequestMethod.POST)
    @Transactional
    public Result registerRestaurant(HttpServletRequest request, @RequestBody Restaurant restaurant) {
        User user =  userRepository.getOne(jwtTokenUtil.getUserId(request));

        restaurant.setOwner(user);
        restaurantRepository.save(restaurant);

        user.getRestaurants().add(restaurant);
        userRepository.save(user);
        return Result.createResult(ResultStatus.OK, restaurant);
    }

    @RequestMapping(path = "/add_restaurant", method = RequestMethod.POST)
    public Result addRestaurantToUser(HttpServletRequest request,
            @RequestParam(name = "telephone") String telephone,
                                @RequestParam(name = "restaurantId") Long restaurant_id) {
        Restaurant restaurant = restaurantRepository.getOne(restaurant_id);
        User owner = restaurant.getOwner();
        if (owner.getId().equals(jwtTokenUtil.getUserId(request))) {
            User newPartner = userRepository.findByTelephone(telephone);
            if (newPartner != null) {
                return Result.createError(ResultStatus.USER_ERROR, new ApplicationException(ErrorCode.NOT_FOUND));
            }
            newPartner.getRestaurants().add(restaurant);
        }
        return Result.createError(ResultStatus.USER_ERROR, new ApplicationException(ErrorCode.PERMISSION_DENIED));
    }
}
