package completo.projeto.completo.mapper;


import completo.projeto.completo.dto.VitalSignRecordDTO;
import completo.projeto.completo.entities.VitalSignRecord;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface VitalSignRecordMapper {

    VitalSignRecordMapper INSTANCE = Mappers.getMapper(VitalSignRecordMapper.class);

    VitalSignRecord toEntity(VitalSignRecordDTO dto);

    VitalSignRecordDTO toDTO(VitalSignRecord entity);
}

