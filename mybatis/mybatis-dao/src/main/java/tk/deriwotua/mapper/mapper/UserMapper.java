package tk.deriwotua.mapper.mapper;

import tk.deriwotua.mapper.domain.User;

import java.util.List;

public interface UserMapper {

    public List<User> findByCondition(User user);

    public List<User> findByIds(List<Integer> ids);


}
