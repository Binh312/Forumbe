package com.web.service;

import com.web.customrepository.CustomBlogRepository;
import com.web.dto.request.FileDto;
import com.web.dto.request.BlogRequest;
import com.web.entity.*;
import com.web.enums.ActiveStatus;
import com.web.exception.MessageException;
import com.web.mapper.BlogMapper;
import com.web.repository.BlogCategoryRepository;
import com.web.repository.BlogFileRepository;
import com.web.repository.BlogRepository;
import com.web.repository.CategoryRepository;
import com.web.utils.Contains;
import com.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BlogService {

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BlogCategoryRepository blogCategoryRepository;

    @Autowired
    private BlogFileRepository blogFileRepository;

    @Autowired
    private BlogMapper blogMapper;

    @Autowired
    private UserUtils userUtils;

    public Blog save(BlogRequest request) {
        if (request.getId() != null) {
            throw new MessageException("Id must null");
        }
        List<Category> categories = new ArrayList<>();
        // kiểm tra xem có danh mục nào không tồn tại không, nếu có thì hủy hàm, báo lỗi
        for (Long id : request.getListCategoryId()) {
            Optional<Category> category = categoryRepository.findById(id);
            if (category.isEmpty()) {
                throw new MessageException("Danh mục :" + id + " không tồn tại");
            }
            categories.add(category.get());
        }

        User user = userUtils.getUserWithAuthority();
        Blog blog = blogMapper.convertRequestToBlog(request);
        blog.setCreatedDate(new Date(System.currentTimeMillis()));
        blog.setCreatedTime(new Time(System.currentTimeMillis()));
        blog.setUser(user);
        blog.setNumLike(0);
        blog.setNumComment(0);
        if(user.getRole().equals(Contains.ROLE_ADMIN)){
            blog.setActived(true);
        }
        Blog result = blogRepository.save(blog);

        for (Category c : categories) {
            BlogCategory blogCategory = new BlogCategory();
            blogCategory.setCategory(c);
            blogCategory.setBlog(result);
            blogCategoryRepository.save(blogCategory);
        }

        for (FileDto blogFileDto : request.getLinkFiles()) {
            BlogFile blogFile = new BlogFile();
            blogFile.setBlog(result);
            blogFile.setLinkFile(blogFileDto.getLinkFile());
            blogFile.setTypeFile(blogFileDto.getTypeFile());
            blogFileRepository.save(blogFile);
        }
        return result;
    }

    public Blog update(BlogRequest request) {
        if (request.getId() == null) {
            throw new MessageException("Id is not null");
        }
        Optional<Blog> blogExist = blogRepository.findById(request.getId());
        if(blogExist.isEmpty()){
            throw new MessageException("blog: "+request.getId()+" not found");
        }
        // nếu user muốn sửa khác với user đăng thì báo lỗi
        User user = userUtils.getUserWithAuthority();

        if (blogExist.get().getUser().getId() != user.getId() && !user.getRole().equals(Contains.ROLE_ADMIN)
                && !user.getRole().equals(Contains.ROLE_BLOG_MANAGER)){
            throw new MessageException("Không đủ quyền");
        }

        List<Category> categories = new ArrayList<>();
        // kiểm tra xem có danh mục nào không tồn tại không, nếu có thì hủy hàm, báo lỗi
        for (Long id : request.getListCategoryId()) {
            Optional<Category> category = categoryRepository.findById(id);
            if (category.isEmpty()) {
                throw new MessageException("Danh mục :" + id + " không tồn tại");
            }
            categories.add(category.get());
        }

        Blog blog = blogMapper.convertRequestToBlog(request);
        blog.setCreatedDate(blogExist.get().getCreatedDate());
        blog.setCreatedTime(blogExist.get().getCreatedTime());
        blog.setUser(blogExist.get().getUser());
        blog.setNumLike(blogExist.get().getNumLike());
        blog.setNumComment(blogExist.get().getNumComment());
        if (blogExist.get().getUser().getId() == user.getId() && !user.getRole().equals(Contains.ROLE_ADMIN)
                && !user.getRole().equals(Contains.ROLE_BLOG_MANAGER)){
            blog.setActived(false);
        } else {
            blog.setActived(blogExist.get().getActived());
        }
        Blog result = blogRepository.save(blog);

        blogCategoryRepository.deleteByBlog(result.getId());
        for (Category c : categories) {
            BlogCategory blogCategory = new BlogCategory();
            blogCategory.setCategory(c);
            blogCategory.setBlog(result);
            blogCategoryRepository.save(blogCategory);
        }

        blogFileRepository.deleteByBlog(request.getId());
        for (FileDto blogFileDto : request.getLinkFiles()) {
            BlogFile blogFile = new BlogFile();
            blogFile.setBlog(result);
            blogFile.setLinkFile(blogFileDto.getLinkFile());
            blogFile.setTypeFile(blogFileDto.getTypeFile());
            blogFileRepository.save(blogFile);
        }
        return result;
    }

    public Blog getBlogById(Long id){
        Optional<Blog> blog = blogRepository.getBlogById(id);
        if (blog.isEmpty()){
            throw new MessageException("Blog không tồn tại");
        }
        return blog.get();
    }

    public Blog findById(Long id){
        Optional<Blog> blog = blogRepository.findById(id);
        if (blog.isEmpty()){
            throw new MessageException("Blog không tồn tại");
        }
        return blog.get();
    }

    public Page<Blog> findAllBlog(Pageable pageable){
        Page<Blog> page = blogRepository.findAllBlog(pageable);
        return page;
    }

    public String deleteBlog(Long blogId){
        Optional<Blog> blogOptional = blogRepository.findById(blogId);
        if(blogOptional.isEmpty()){
            throw new MessageException("blog id không tồn tại!");
        }

        // lấy thông tin user đang đăng nhập (user gửi yêu cầu)
        User user = userUtils.getUserWithAuthority();

        if (blogOptional.get().getUser().getId() != user.getId() && !user.getRole().equals(Contains.ROLE_ADMIN)
                && !user.getRole().equals(Contains.ROLE_BLOG_MANAGER)){
            throw new MessageException("Không đủ quyền");
        }

        blogRepository.delete(blogOptional.get());
        return "Đã xóa bài viết thành công";
    }

    public Page<Blog> getBlogByUser(Long userId, Pageable pageable){
        Page<Blog> page = blogRepository.getBlogByUser(userId,pageable);
        return page;
    }

    public Page<Blog> getBlogActived(String keywords, Integer currentPage, Integer size){
        Pageable pageable = PageRequest.of(currentPage - 1,size);
        Specification<Blog> specification = CustomBlogRepository.filter(keywords);
        Page<Blog> page = blogRepository.findAll(specification, pageable);
        return page;
    }

    public Page<Blog> getBlogUnActived(Pageable pageable){
        Page<Blog> page = blogRepository.getBlogUnActived(pageable);
        return page;
    }

    public Page<Blog> searchBlogActived(String searchTitle, String userName, Pageable pageable){
        if (searchTitle.isEmpty() && userName.isEmpty()) {
            return blogRepository.getBlogActived(pageable);
        } else {
            return blogRepository.searchBlogActived(searchTitle,userName,pageable);
        }
    }

    public Page<Blog> getBlogByCategory(Long categoryId, Pageable pageable){
        Page<Blog> page = blogRepository.getBlogByCategory(categoryId, pageable);
        return page;
    }

    public Page<Blog> getTop10Blog(Pageable pageable){
        return blogRepository.getTop10Blog(pageable);
    }

    public ActiveStatus activeOrUnactive(Long blogId){
        Optional<Blog> blog = blogRepository.findById(blogId);
        if (blog.isEmpty()){
            throw new MessageException("Blog này không tồn tại!");
        }
        if (blog.get().getActived() == true){
            blog.get().setActived(false);
            blogRepository.save(blog.get());
            return ActiveStatus.DA_KHOA;
        } else {
            blog.get().setActived(true);
            blogRepository.save(blog.get());
            return ActiveStatus.DA_MO_KHOA;
        }
    }

    public Page<Blog> adminSearchBlogByTitle(String searchTitle, Pageable pageable){
        if (searchTitle.isEmpty()){
            return blogRepository.findAllBlog(pageable);
        } else {
            return blogRepository.adminSearchBlogByTitle(searchTitle,pageable);
        }
    }
}
