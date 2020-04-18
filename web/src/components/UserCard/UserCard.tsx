import React from 'react'

interface UserCardProps {
  bgImage: string
  avatar: string
  name: string
  userName: string
  description: JSX.Element
  socials: JSX.Element
}

export const UserCard = (props: UserCardProps) => {
  const { socials, userName, name, bgImage, description, avatar } = props
  return (
    <div className="card card-user">
      <div className="image">
        <img src={bgImage} alt="..." />
      </div>
      <div className="content">
        <div className="author">
          <img className="avatar border-gray" src={avatar} alt="..." />
          <h4 className="title">
            {name}
            <br />
            <small>{userName}</small>
          </h4>
        </div>
        <p className="description text-center">{description}</p>
      </div>
      <hr />
      <div className="text-center">{socials}</div>
    </div>
  )
}

export default UserCard
